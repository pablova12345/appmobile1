package bo.gob.gamc.catastro.digid.data.repository

import android.net.Uri
import bo.gob.gamc.catastro.digid.core.network.TokenManager
import bo.gob.gamc.catastro.digid.core.storage.PdfManager
import bo.gob.gamc.catastro.digid.core.storage.UserStorageManager
import bo.gob.gamc.catastro.digid.data.local.dao.DocumentDao
import bo.gob.gamc.catastro.digid.data.local.entities.DocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepository @Inject constructor(
    private val documentDao: DocumentDao,
    private val storageManager: UserStorageManager,
    private val pdfManager: PdfManager,
    private val tokenManager: TokenManager
) {
    fun getDocuments(): Flow<List<DocumentEntity>> {
        val userId = tokenManager.getUserId()
        return documentDao.getDocumentsByUser(userId)
    }

    fun searchDocuments(query: String): Flow<List<DocumentEntity>> {
        val userId = tokenManager.getUserId()
        return documentDao.searchDocuments(userId, query)
    }

    suspend fun saveScannedImagesAsPdf(
        imageFiles: List<File>,
        descripcion: String,
        tramite: String?
    ): Result<DocumentEntity> = withContext(Dispatchers.IO) {
        try {
            if (imageFiles.isEmpty()) {
                return@withContext Result.failure(Exception("No hay imágenes escaneadas."))
            }

            val userId = tokenManager.getUserId()
            val pdfFile = pdfManager.createPdfFromImages(imageFiles, "doc_${userId.take(6)}")

            // Guardar páginas individuales JPEG para consumo de la IA
            storageManager.savePagesForDocument(imageFiles, pdfFile.name)

            val entity = DocumentEntity(
                fileName = pdfFile.name,
                filePath = pdfFile.absolutePath,
                descripcion = descripcion.ifBlank { "Sin descripción" },
                tramiteAsociado = tramite?.takeIf { it.isNotBlank() },
                userId = userId,
                sizeBytes = pdfFile.length()
            )

            val id = documentDao.insertDocument(entity)
            Result.success(entity.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveAttachedFile(
        sourceUri: Uri,
        fileName: String,
        descripcion: String,
        tramite: String?
    ): Result<DocumentEntity> = withContext(Dispatchers.IO) {
        try {
            val copiedFile = storageManager.copyFileToUserSandbox(sourceUri, fileName)
            val userId = tokenManager.getUserId()

            val entity = DocumentEntity(
                fileName = copiedFile.name,
                filePath = copiedFile.absolutePath,
                descripcion = descripcion.ifBlank { "Archivo adjunto" },
                tramiteAsociado = tramite?.takeIf { it.isNotBlank() },
                userId = userId,
                sizeBytes = copiedFile.length()
            )

            val id = documentDao.insertDocument(entity)
            Result.success(entity.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDocument(document: DocumentEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(document.filePath)
            storageManager.deleteDocumentAndPages(file)
            documentDao.deleteDocument(document)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getPagesForDocument(pdfFileName: String): List<File> {
        return storageManager.getPagesForDocument(pdfFileName)
    }
}
