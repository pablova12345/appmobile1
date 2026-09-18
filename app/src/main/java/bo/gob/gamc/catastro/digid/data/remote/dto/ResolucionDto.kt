package bo.gob.gamc.catastro.digid.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResolucionDto(
    @SerialName("resolution_id") val idResolucion: String,
    @SerialName("resolution_number") val nroResolucion: String,
    @SerialName("name") val nombre: String,
    @SerialName("status") val estado: String,
    @SerialName("total_pages") val totalPaginas: Int = 0,
    @SerialName("created_at") val fechaCreacion: String = ""
)
