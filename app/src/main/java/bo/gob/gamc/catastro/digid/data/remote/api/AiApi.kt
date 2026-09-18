package bo.gob.gamc.catastro.digid.data.remote.api

import bo.gob.gamc.catastro.digid.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AiApi {
    @GET("api/catalog")
    suspend fun getCatalog(): Response<AiCatalogResponse>

    @POST("api/classify")
    suspend fun classifyDocument(@Body request: AiClassifyRequest): Response<AiClassifyResponse>

    @POST("api/answer")
    suspend fun askQuestion(@Body request: AiAnswerRequest): Response<AiAnswerResponse>
}
