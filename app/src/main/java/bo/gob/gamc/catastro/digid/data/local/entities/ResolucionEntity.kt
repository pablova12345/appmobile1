package bo.gob.gamc.catastro.digid.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resoluciones")
data class ResolucionEntity(
    @PrimaryKey val idResolucion: String,
    val nroResolucion: String,
    val nombre: String,
    val estado: String, // "pendiente_ocr", "en_proceso", "listo"
    val totalPaginas: Int,
    val fechaCreacion: String,
    val userId: String
)
