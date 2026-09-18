package bo.gob.gamc.catastro.digid.presentation.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import bo.gob.gamc.catastro.digid.R
import bo.gob.gamc.catastro.digid.core.security.BiometricHelper
import bo.gob.gamc.catastro.digid.core.theme.*
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val biometricHelper = remember { BiometricHelper(context) }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AuthUiEvent.LoginSuccess -> onLoginSuccess()
                is AuthUiEvent.ShowError -> snackbarMessage = event.message
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo / Banner GAMC
            Image(
                painter = painterResource(id = R.drawable.banner),
                contentDescription = "Alcaldía de Cochabamba",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "Digitalización SisCat",
                style = Typography.headlineMedium,
                color = Secondary
            )
            Text(
                text = "Acceso institucional seguro",
                style = Typography.bodySmall,
                modifier = Modifier.padding(bottom = 28.dp)
            )

            // Input Usuario
            OutlinedTextField(
                value = state.username,
                onValueChange = { viewModel.onUsernameChange(it) },
                label = { Text("Usuario") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryDeep) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryDeep,
                    unfocusedBorderColor = Border,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Input Contraseña
            OutlinedTextField(
                value = state.password,
                onValueChange = { viewModel.onPasswordChange(it) },
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryDeep) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    viewModel.login()
                }),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryDeep,
                    unfocusedBorderColor = Border,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.errorMessage!!,
                    style = Typography.bodySmall,
                    color = StatusDanger,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón de Inicio de Sesión
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.login()
                },
                enabled = !state.isLoading,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDeep),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = TextOnDark, modifier = Modifier.size(24.dp))
                } else {
                    Text("Ingresar", style = Typography.labelLarge)
                }
            }

            // Opción Biometría (Huella / Rostro)
            if (state.isBiometricAvailable) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        val activity = context as? FragmentActivity
                        if (activity != null) {
                            biometricHelper.promptBiometric(
                                activity = activity,
                                onSuccess = { viewModel.loginWithBiometrics() },
                                onError = { snackbarMessage = it }
                            )
                        } else {
                            viewModel.loginWithBiometrics()
                        }
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometría",
                        tint = Secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ingresar con Biometría", color = Secondary, style = Typography.bodyMedium)
                }
            }
        }

        snackbarMessage?.let { msg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { snackbarMessage = null }) {
                        Text("OK", color = TextOnDark)
                    }
                }
            ) {
                Text(msg)
            }
        }
    }
}
