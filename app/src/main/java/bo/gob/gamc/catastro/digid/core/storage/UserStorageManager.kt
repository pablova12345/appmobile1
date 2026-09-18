package bo.gob.gamc.catastro.digid.core.storage

import android.content.Context
import android.net.Uri
import bo.gob.gamc.catastro.digid.core.network.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserStorageManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenManager: TokenManager
) {
    fun getUserDirectory(): File {
        val rawId = tokenManager.getUserId()
        val sanitized = rawId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val dir = File(context.filesDir, "usuarios/$sanitized")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getPagesDirectory(): File {
        val dir = File(getUserDirectory(), "paginas")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun copyFileToUserSandbox(sourceUri: Uri, originalName: String): File {
        val userDir = getUserDirectory()
        val ext = originalName.substringAfterLast(".", "pdf")
        val targetFile = File(userDir, "archivo_${System.currentTimeMillis()}.$ext")

        val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
        val outputStream = FileOutputStream(targetFile)

        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        return targetFile
    }

    fun savePagesForDocument(imageFiles: List<File>, pdfBaseName: String): List<File> {
        val pagesDir = getPagesDirectory()
        val prefix = pdfBaseName.removeSuffix(".pdf") + "__p"
        val saved = mutableListOf<File>()

        imageFiles.forEachIndexed { index, img ->
            val dest = File(pagesDir, "${prefix}${index + 1}.jpg")
            img.copyTo(dest, overwrite = true)
            saved.add(dest)
        }
        return saved
    }

    fun getPagesForDocument(pdfBaseName: String): List<File> {
        val pagesDir = getPagesDirectory()
        val prefix = pdfBaseName.removeSuffix(".pdf") + "__p"
        return pagesDir.listFiles { file ->
            file.name.startsWith(prefix) && file.name.endsWith(".jpg")
        }?.sortedBy { it.name } ?: emptyList()
    }

    fun deleteDocumentAndPages(file: File) {
        val pdfName = file.name
        val pages = getPagesForDocument(pdfName)
        pages.forEach { it.delete() }
        if (file.exists()) {
            file.delete()
        }
    }
}
