package bo.gob.gamc.catastro.digid.data.remote.api

import bo.gob.gamc.catastro.digid.data.remote.dto.GeoextractDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface GeoextractApi {
    @GET("api/geoextraction/captures")
    suspend fun listCapturas(): Response<List<GeoextractDto>>

    @Multipart
    @POST("api/geoextraction/captures")
    suspend fun uploadCaptura(
        @Part file: MultipartBody.Part
    ): Response<GeoextractDto>
}
