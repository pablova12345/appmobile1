package bo.gob.gamc.catastro.digid.presentation.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.gob.gamc.catastro.digid.core.network.NetworkMonitor
import bo.gob.gamc.catastro.digid.data.local.entities.DocumentEntity
import bo.gob.gamc.catastro.digid.data.repository.AuthRepository
import bo.gob.gamc.catastro.digid.data.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val authRepository: AuthRepository,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val searchQuery = MutableStateFlow("")
    val nroTramite = MutableStateFlow("")
    val isLoading = MutableStateFlow(false)

    val documents: StateFlow<List<DocumentEntity>> = combine(
        searchQuery,
        nroTramite,
        documentRepository.getDocuments()
    ) { query, tramite, list ->
        var filtered = list
        if (tramite.isNotBlank()) {
            filtered = filtered.filter { it.tramiteAsociado.equals(tramite.trim(), ignoreCase = true) }
        }
        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.fileName.contains(query, ignoreCase = true) ||
                        it.descripcion.contains(query, ignoreCase = true)
            }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Estado temporal para el diálogo de descripción
    var pendingScanImages: List<File> = emptyList()
    var pendingAttachedFile: Pair<Uri, String>? = null

    fun onSearchChange(q: String) {
        searchQuery.value = q
    }

    fun onTramiteChange(t: String) {
        nroTramite.value = t
    }

    fun savePendingScan(descripcion: String) {
        if (pendingScanImages.isEmpty()) return
        viewModelScope.launch {
            isLoading.value = true
            documentRepository.saveScannedImagesAsPdf(
                imageFiles = pendingScanImages,
                descripcion = descripcion,
                tramite = nroTramite.value
            )
            pendingScanImages = emptyList()
            isLoading.value = false
        }
    }

    fun savePendingAttachment(descripcion: String) {
        val attached = pendingAttachedFile ?: return
        viewModelScope.launch {
            isLoading.value = true
            documentRepository.saveAttachedFile(
                sourceUri = attached.first,
                fileName = attached.second,
                descripcion = descripcion,
                tramite = nroTramite.value
            )
            pendingAttachedFile = null
            isLoading.value = false
        }
    }

    fun deleteDocument(document: DocumentEntity) {
        viewModelScope.launch {
            documentRepository.deleteDocument(document)
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
