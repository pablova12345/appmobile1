package bo.gob.gamc.catastro.digid.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.gob.gamc.catastro.digid.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val errorMessage: String? = null
)

sealed class AuthUiEvent {
    object LoginSuccess : AuthUiEvent()
    data class ShowError(val message: String) : AuthUiEvent()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AuthUiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        _uiState.value = _uiState.value.copy(
            isBiometricAvailable = authRepository.isBiometricAvailable()
        )
    }

    fun onUsernameChange(u: String) {
        _uiState.value = _uiState.value.copy(username = u, errorMessage = null)
    }

    fun onPasswordChange(p: String) {
        _uiState.value = _uiState.value.copy(password = p, errorMessage = null)
    }

    fun login() {
        val current = _uiState.value
        if (current.username.isBlank() || current.password.isBlank()) {
            _uiState.value = current.copy(errorMessage = "Por favor ingrese usuario y contraseña")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.login(current.username.trim(), current.password)
            _uiState.value = _uiState.value.copy(isLoading = false)

            result.onSuccess {
                _eventFlow.emit(AuthUiEvent.LoginSuccess)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(errorMessage = err.message)
                _eventFlow.emit(AuthUiEvent.ShowError(err.message ?: "Error de autenticación"))
            }
        }
    }

    fun loginWithBiometrics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.loginWithBiometrics()
            _uiState.value = _uiState.value.copy(isLoading = false)

            result.onSuccess {
                _eventFlow.emit(AuthUiEvent.LoginSuccess)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(errorMessage = err.message)
                _eventFlow.emit(AuthUiEvent.ShowError(err.message ?: "Error biométrico"))
            }
        }
    }
}
