package bo.gob.gamc.catastro.digid.core.network

import bo.gob.gamc.catastro.digid.BuildConfig
import okhttp3.Authenticator
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeycloakAuthenticator @Inject constructor(
    private val tokenManager: TokenManager
) : Authenticator {

    private val refreshClient = OkHttpClient.Builder().build()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Evitar bucles infinitos si falla el refresh
        if (responseCount(response) >= 3) return null

        synchronized(this) {
            val currentToken = tokenManager.getAccessToken()
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

            // Si otro hilo ya actualizó el token, reintentar con el nuevo token
            if (currentToken != null && currentToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshToken = tokenManager.getRefreshToken() ?: return null

            val tokenEndpoint = "${BuildConfig.KEYCLOAK_BASE_URL}/realms/${BuildConfig.KEYCLOAK_REALM}/protocol/openid-connect/token"
            val bodyBuilder = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .add("client_id", BuildConfig.KEYCLOAK_CLIENT_ID)

            if (BuildConfig.KEYCLOAK_CLIENT_SECRET.isNotEmpty()) {
                bodyBuilder.add("client_secret", BuildConfig.KEYCLOAK_CLIENT_SECRET)
            }

            val refreshRequest = Request.Builder()
                .url(tokenEndpoint)
                .post(bodyBuilder.build())
                .build()

            try {
                val refreshResponse = refreshClient.newCall(refreshRequest).execute()
                if (refreshResponse.isSuccessful) {
                    val resBody = refreshResponse.body?.string() ?: return null
                    val json = JSONObject(resBody)
                    val newAccessToken = json.getString("access_token")
                    val newRefreshToken = json.optString("refresh_token", refreshToken)

                    tokenManager.saveTokens(newAccessToken, newRefreshToken)

                    return response.request.newBuilder()
                        .header("Authorization", "Bearer $newAccessToken")
                        .build()
                } else {
                    tokenManager.clearSession()
                    return null
                }
            } catch (e: Exception) {
                return null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}
