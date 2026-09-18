package bo.gob.gamc.catastro.digid.presentation.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class PdfViewerUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val currentPageBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val isPdf: Boolean = true
)

@HiltViewModel
class PdfViewerViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(PdfViewerUiState())
    val uiState = _uiState.asStateFlow()

    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null

    fun loadDocument(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return

        val isPdf = filePath.endsWith(".pdf", ignoreCase = true)
        _uiState.value = _uiState.value.copy(isPdf = isPdf)

        if (isPdf) {
            viewModelScope.launch(Dispatchers.IO) {
                _uiState.value = _uiState.value.copy(isLoading = true)
                try {
                    fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    fileDescriptor?.let {
                        pdfRenderer = PdfRenderer(it)
                        val pages = pdfRenderer?.pageCount ?: 0
                        _uiState.value = _uiState.value.copy(
                            totalPages = pages,
                            currentPage = 0
                        )
                        renderPage(0)
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun nextPage() {
        val next = _uiState.value.currentPage + 1
        if (next < _uiState.value.totalPages) {
            renderPage(next)
        }
    }

    fun prevPage() {
        val prev = _uiState.value.currentPage - 1
        if (prev >= 0) {
            renderPage(prev)
        }
    }

    private fun renderPage(pageIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val renderer = pdfRenderer ?: return@launch
            try {
                val page = renderer.openPage(pageIndex)
                val width = page.width * 2
                val height = page.height * 2
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                _uiState.value = _uiState.value.copy(
                    currentPage = pageIndex,
                    currentPageBitmap = bitmap,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            pdfRenderer?.close()
            fileDescriptor?.close()
        } catch (_: Exception) {}
    }
}
