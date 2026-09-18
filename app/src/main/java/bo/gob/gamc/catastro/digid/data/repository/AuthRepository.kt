package bo.gob.gamc.catastro.digid.data.repository

import bo.gob.gamc.catastro.digid.BuildConfig
import bo.gob.gamc.catastro.digid.core.network.TokenManager
import bo.gob.gamc.catastro.digid.core.security.BiometricHelper
import bo.gob.gamc.catastro.digid.data.remote.api.KeycloakApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val keycloakApi: KeycloakApi,
    private val tokenManager: TokenManager,
    private val biometricHelper: BiometricHelper
) {
    fun isAuthenticated(): Boolean = tokenManager.getAccessToken() != null

    fun getUserId(): String = tokenManager.getUserId()

    fun getUsername(): String? = tokenManager.getUsername()

    fun isBiometricAvailable(): Boolean = biometricHelper.isBiometricAvailable()

    suspend fun login(username: String, pass: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val params = mutableMapOf(
                "grant_type" to "password",
                "username" to username,
                "password" to pass,
                "client_id" to BuildConfig.KEYCLOAK_CLIENT_ID,
                "scope" to "openid profile"
            )
            if (BuildConfig.KEYCLOAK_CLIENT_SECRET.isNotEmpty()) {
                params["client_secret"] = BuildConfig.KEYCLOAK_CLIENT_SECRET
            }

            val response = keycloakApi.login(BuildConfig.KEYCLOAK_REALM, params)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                tokenManager.saveTokens(body.accessToken, body.refreshToken)
                tokenManager.saveCredentialsForBiometrics(username, pass)
                Result.success(Unit)
            } else {
                val errCode = response.code()
                val message = when (errCode) {
                    400, 401 -> "Usuario o contraseña incorrectos."
                    else -> "Error de autenticación ($errCode)."
                }
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo contactar al servidor de autenticación."))
        }
    }

    suspend fun loginWithBiometrics(): Result<Unit> {
        val creds = tokenManager.getSavedCredentials()
            ?: return Result.failure(Exception("No hay credenciales guardadas. Inicie sesión manualmente."))
        return login(creds.first, creds.second)
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            val rt = tokenManager.getRefreshToken()
            if (rt != null) {
                val params = mutableMapOf(
                    "refresh_token" to rt,
                    "client_id" to BuildConfig.KEYCLOAK_CLIENT_ID
                )
                if (BuildConfig.KEYCLOAK_CLIENT_SECRET.isNotEmpty()) {
                    params["client_secret"] = BuildConfig.KEYCLOAK_CLIENT_SECRET
                }
                keycloakApi.logout(BuildConfig.KEYCLOAK_REALM, params)
            }
        } catch (_: Exception) {
        } finally {
            tokenManager.clearSession()
        }
    }
}
