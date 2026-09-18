package bo.gob.gamc.catastro.digid.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import bo.gob.gamc.catastro.digid.data.remote.api.AiApi
import bo.gob.gamc.catastro.digid.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class AiRepository @Inject constructor(
    private val aiApi: AiApi
) {
    suspend fun getCatalog(): Result<List<AiTipoDocumentoDto>> = withContext(Dispatchers.IO) {
        try {
            val res = aiApi.getCatalog()
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!.tipos)
            } else {
                Result.failure(Exception("Error al cargar catálogo IA (${res.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun classifyDocument(imageFile: File): Result<AiClassifyResponse> = withContext(Dispatchers.IO) {
        try {
            val base64 = resizeAndEncodeBase64(imageFile, maxWidth = 1600)
            val request = AiClassifyRequest(images = listOf(base64))
            val res = aiApi.classifyDocument(request)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Error al clasificar: ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun askQuestion(imageFile: File, tipo: String, pregunta: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val base64 = resizeAndEncodeBase64(imageFile, maxWidth = 1600)
            val request = AiAnswerRequest(
                images = listOf(base64),
                tipo = tipo,
                pregunta = pregunta
            )
            val res = aiApi.askQuestion(request)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!.respuesta)
            } else {
                Result.failure(Exception("Error en respuesta IA: ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun resizeAndEncodeBase64(imageFile: File, maxWidth: Int): String {
        val originalBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        val bitmap = if (originalBitmap.width > maxWidth) {
            val ratio = maxWidth.toFloat() / originalBitmap.width
            val newHeight = (originalBitmap.height * ratio).roundToInt()
            val scaled = Bitmap.createScaledBitmap(originalBitmap, maxWidth, newHeight, true)
            originalBitmap.recycle()
            scaled
        } else {
            originalBitmap
        }

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val bytes = stream.toByteArray()
        bitmap.recycle()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
