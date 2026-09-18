package bo.gob.gamc.catastro.digid.data.remote.api

import bo.gob.gamc.catastro.digid.data.remote.dto.ResolucionDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ResolucionesApi {
    @GET("api/resolutions")
    suspend fun listResoluciones(): Response<List<ResolucionDto>>

    @Multipart
    @POST("api/resolutions")
    suspend fun uploadResolucion(
        @Part("resolution_number") nroResolucion: RequestBody,
        @Part("name") nombre: RequestBody,
        @Part paginas: List<MultipartBody.Part>
    ): Response<ResolucionDto>
}
