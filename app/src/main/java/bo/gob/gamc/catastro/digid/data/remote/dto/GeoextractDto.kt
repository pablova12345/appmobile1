package bo.gob.gamc.catastro.digid.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeoextractDto(
    @SerialName("id_captura") val idCaptura: String = "",
    val mime: String = "image/jpeg",
    @SerialName("fecha_creacion") val fechaCreacion: String? = null
)
