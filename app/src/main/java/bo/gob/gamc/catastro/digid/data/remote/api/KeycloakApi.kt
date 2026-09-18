package bo.gob.gamc.catastro.digid.data.remote.api

import bo.gob.gamc.catastro.digid.data.remote.dto.KeycloakTokenResponse
import retrofit2.Response
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Path

interface KeycloakApi {
    @FormUrlEncoded
    @POST("realms/{realm}/protocol/openid-connect/token")
    suspend fun login(
        @Path("realm") realm: String,
        @FieldMap params: Map<String, String>
    ): Response<KeycloakTokenResponse>

    @FormUrlEncoded
    @POST("realms/{realm}/protocol/openid-connect/logout")
    suspend fun logout(
        @Path("realm") realm: String,
        @FieldMap params: Map<String, String>
    ): Response<Unit>
}
