package bo.gob.gamc.catastro.digid.presentation.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.gob.gamc.catastro.digid.data.remote.dto.AiTipoDocumentoDto
import bo.gob.gamc.catastro.digid.data.repository.AiRepository
import bo.gob.gamc.catastro.digid.data.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class AnswerItem(
    val pregunta: String,
    val respuesta: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class AIStudyUiState(
    val pages: List<File> = emptyList(),
    val catalog: List<AiTipoDocumentoDto> = emptyList(),
    val isClassifying: Boolean = false,
    val classifiedType: String? = null,
    val confidence: Double = 0.0,
    val classificationReason: String? = null,
    val classificationError: String? = null,
    val answers: List<AnswerItem> = emptyList()
)

@HiltViewModel
class AIStudyViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIStudyUiState())
    val uiState = _uiState.asStateFlow()

    fun initForDocument(fileName: String) {
        val pages = documentRepository.getPagesForDocument(fileName)
        _uiState.value = _uiState.value.copy(pages = pages)

        loadCatalogAndClassify(pages.firstOrNull())
    }

    private fun loadCatalogAndClassify(firstPage: File?) {
        viewModelScope.launch {
            val catalogRes = aiRepository.getCatalog()
            catalogRes.onSuccess { catalog ->
                _uiState.value = _uiState.value.copy(catalog = catalog)
            }

            if (firstPage != null && firstPage.exists()) {
                _uiState.value = _uiState.value.copy(isClassifying = true, classificationError = null)
                val classifyRes = aiRepository.classifyDocument(firstPage)
                classifyRes.onSuccess { res ->
                    _uiState.value = _uiState.value.copy(
                        isClassifying = false,
                        classifiedType = res.tipo,
                        confidence = res.confianza,
                        classificationReason = res.razon
                    )
                }.onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isClassifying = false,
                        classificationError = err.message ?: "No se pudo clasificar el documento."
                    )
                }
            }
        }
    }

    fun askQuestion(pregunta: String) {
        val firstPage = _uiState.value.pages.firstOrNull() ?: return
        val tipo = _uiState.value.classifiedType ?: return

        val newAnswers = _uiState.value.answers.toMutableList().apply {
            add(AnswerItem(pregunta = pregunta, isLoading = true))
        }
        _uiState.value = _uiState.value.copy(answers = newAnswers)

        viewModelScope.launch {
            val result = aiRepository.askQuestion(firstPage, tipo, pregunta)
            val updated = _uiState.value.answers.map { item ->
                if (item.pregunta == pregunta && item.isLoading) {
                    result.fold(
                        onSuccess = { item.copy(respuesta = it, isLoading = false) },
                        onFailure = { item.copy(error = it.message ?: "Error al responder", isLoading = false) }
                    )
                } else item
            }
            _uiState.value = _uiState.value.copy(answers = updated)
        }
    }
}
