package bo.gob.gamc.catastro.digid.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KeycloakTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("refresh_expires_in") val refreshExpiresIn: Long? = null,
    @SerialName("token_type") val tokenType: String? = null
)
