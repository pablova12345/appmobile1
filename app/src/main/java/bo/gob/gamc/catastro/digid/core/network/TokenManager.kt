package bo.gob.gamc.catastro.digid.core.network

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "digid_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveTokens(accessToken: String, refreshToken: String?) {
        val editor = prefs.edit()
            .putString("access_token", accessToken)
        if (refreshToken != null) {
            editor.putString("refresh_token", refreshToken)
        }
        val sub = extractSubFromJwt(accessToken)
        if (sub != null) {
            editor.putString("user_id", sub)
        }
        val username = extractUsernameFromJwt(accessToken)
        if (username != null) {
            editor.putString("username", username)
        }
        editor.apply()
    }

    fun getAccessToken(): String? = prefs.getString("access_token", null)

    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)

    fun getUserId(): String = prefs.getString("user_id", "_sin_sesion") ?: "_sin_sesion"

    fun getUsername(): String? = prefs.getString("username", null)

    fun saveCredentialsForBiometrics(user: String, pass: String) {
        prefs.edit()
            .putString("bio_user", user)
            .putString("bio_pass", pass)
            .apply()
    }

    fun getSavedCredentials(): Pair<String, String>? {
        val u = prefs.getString("bio_user", null)
        val p = prefs.getString("bio_pass", null)
        return if (u != null && p != null) Pair(u, p) else null
    }

    fun clearSession() {
        prefs.edit()
            .remove("access_token")
            .remove("refresh_token")
            .remove("user_id")
            .remove("username")
            .apply()
    }

    private fun extractSubFromJwt(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val decoded = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP))
            val json = JSONObject(decoded)
            json.optString("sub", null)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractUsernameFromJwt(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val decoded = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP))
            val json = JSONObject(decoded)
            json.optString("preferred_username", null) ?: json.optString("username", null)
        } catch (e: Exception) {
            null
        }
    }
}
