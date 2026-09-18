package bo.gob.gamc.catastro.digid.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documentos")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val descripcion: String,
    val tramiteAsociado: String?,
    val userId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0
)
