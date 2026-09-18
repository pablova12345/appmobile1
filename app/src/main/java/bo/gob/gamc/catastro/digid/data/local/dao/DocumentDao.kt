package bo.gob.gamc.catastro.digid.data.local.dao

import androidx.room.*
import bo.gob.gamc.catastro.digid.data.local.entities.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documentos WHERE userId = :userId ORDER BY createdAt DESC")
    fun getDocumentsByUser(userId: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documentos WHERE userId = :userId AND (fileName LIKE '%' || :query || '%' OR descripcion LIKE '%' || :query || '%') ORDER BY createdAt DESC")
    fun searchDocuments(userId: String, query: String): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("DELETE FROM documentos WHERE filePath = :filePath")
    suspend fun deleteByPath(filePath: String)

    @Query("SELECT * FROM documentos WHERE filePath = :filePath LIMIT 1")
    suspend fun getByPath(filePath: String): DocumentEntity?
}
