package bo.gob.gamc.catastro.digid.presentation.geoextract

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.gob.gamc.catastro.digid.BuildConfig
import bo.gob.gamc.catastro.digid.core.network.PresenceSocket
import bo.gob.gamc.catastro.digid.core.network.PresenceSocketFactory
import bo.gob.gamc.catastro.digid.data.remote.dto.GeoextractDto
import bo.gob.gamc.catastro.digid.data.repository.GeoextractRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class GeoextractUiState(
    val pendientes: List<GeoextractDto> = emptyList(),
    val isUploading: Boolean = false,
    val isRefreshing: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class GeoextractViewModel @Inject constructor(
    private val geoextractRepository: GeoextractRepository,
    presenceSocketFactory: PresenceSocketFactory
) : ViewModel() {

    private val _uiState = MutableStateFlow(GeoextractUiState())
    val uiState = _uiState.asStateFlow()

    private val presenceSocket: PresenceSocket = presenceSocketFactory.create(
        baseUrl = BuildConfig.IDEC_ERP_BACKEND_URL,
        wsPath = "api/geoextraction/captures/ws"
    )

    init {
        refresh()
        presenceSocket.start()
    }

    override fun onCleared() {
        presenceSocket.stop()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            val result = geoextractRepository.listPendientes()
            result.onSuccess { list ->
                _uiState.value = _uiState.value.copy(pendientes = list, isRefreshing = false)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    errorMessage = "No se pudo consultar el ERP. Verifique que idec-erp-back esté activo."
                )
            }
        }
    }

    fun uploadCaptura(imageFile: File) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploading = true, errorMessage = null, successMessage = null)
            val result = geoextractRepository.uploadCaptura(imageFile)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    successMessage = "Foto enviada al ERP con éxito. Continúe desde la web para extraer coordenadas."
                )
                refresh()
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    errorMessage = err.message ?: "Error al enviar la captura al ERP"
                )
            }
        }
    }

    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message, successMessage = null)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
