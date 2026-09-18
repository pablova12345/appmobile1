package bo.gob.gamc.catastro.digid.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AiCatalogResponse(
    val tipos: List<AiTipoDocumentoDto> = emptyList()
)

@Serializable
data class AiTipoDocumentoDto(
    val code: String,
    val title: String,
    val description: String = "",
    val questions: List<String> = emptyList()
)

@Serializable
data class AiClassifyRequest(
    val images: List<String> // base64 strings
)

@Serializable
data class AiClassifyResponse(
    val tipo: String,
    val confianza: Double = 0.0,
    val razon: String = ""
)

@Serializable
data class AiAnswerRequest(
    val images: List<String>,
    val tipo: String,
    val pregunta: String
)

@Serializable
data class AiAnswerResponse(
    val respuesta: String
)
