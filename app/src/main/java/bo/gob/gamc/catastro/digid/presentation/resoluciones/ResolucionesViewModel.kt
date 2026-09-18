package bo.gob.gamc.catastro.digid.presentation.resoluciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.gob.gamc.catastro.digid.BuildConfig
import bo.gob.gamc.catastro.digid.core.network.PresenceSocket
import bo.gob.gamc.catastro.digid.core.network.PresenceSocketFactory
import bo.gob.gamc.catastro.digid.data.local.entities.ResolucionEntity
import bo.gob.gamc.catastro.digid.data.repository.ResolucionesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ResolucionesUiState(
    val nroResolucion: String = "",
    val nombre: String = "",
    val scannedPages: List<File> = emptyList(),
    val isUploading: Boolean = false,
    val isRefreshing: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ResolucionesViewModel @Inject constructor(
    private val resolucionesRepository: ResolucionesRepository,
    presenceSocketFactory: PresenceSocketFactory
) : ViewModel() {

    val resoluciones: StateFlow<List<ResolucionEntity>> = resolucionesRepository.getLocalResoluciones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState = MutableStateFlow(ResolucionesUiState())

    private val presenceSocket: PresenceSocket = presenceSocketFactory.create(
        baseUrl = BuildConfig.ERP_BACKEND_URL,
        wsPath = "api/resolutions/ws"
    )

    init {
        refresh()
        presenceSocket.start()
    }

    override fun onCleared() {
        presenceSocket.stop()
    }

    fun onNroResolucionChange(v: String) {
        uiState.value = uiState.value.copy(nroResolucion = v, errorMessage = null)
    }

    fun onNombreChange(v: String) {
        uiState.value = uiState.value.copy(nombre = v, errorMessage = null)
    }

    fun addScannedPages(pages: List<File>) {
        uiState.value = uiState.value.copy(
            scannedPages = uiState.value.scannedPages + pages
        )
    }

    fun removePage(index: Int) {
        val updated = uiState.value.scannedPages.toMutableList()
        if (index in updated.indices) {
            updated.removeAt(index)
            uiState.value = uiState.value.copy(scannedPages = updated)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            uiState.value = uiState.value.copy(isRefreshing = true)
            resolucionesRepository.refreshResoluciones()
            uiState.value = uiState.value.copy(isRefreshing = false)
        }
    }

    fun upload() {
        val current = uiState.value
        if (current.nroResolucion.isBlank()) {
            uiState.value = current.copy(errorMessage = "Ingrese el N° de resolución")
            return
        }
        if (current.nombre.isBlank()) {
            uiState.value = current.copy(errorMessage = "Ingrese un nombre o referencia")
            return
        }
        if (current.scannedPages.isEmpty()) {
            uiState.value = current.copy(errorMessage = "Escanee al menos una página")
            return
        }

        viewModelScope.launch {
            uiState.value = current.copy(isUploading = true, errorMessage = null, successMessage = null)
            val result = resolucionesRepository.uploadResolucion(
                nroResolucion = current.nroResolucion.trim(),
                nombre = current.nombre.trim(),
                imageFiles = current.scannedPages
            )
            result.onSuccess {
                uiState.value = ResolucionesUiState(
                    successMessage = "¡Resolución subida exitosamente! Continúe el OCR desde la web."
                )
                refresh()
            }.onFailure { err ->
                uiState.value = uiState.value.copy(
                    isUploading = false,
                    errorMessage = err.message ?: "Error al subir resolución"
                )
            }
        }
    }
}
