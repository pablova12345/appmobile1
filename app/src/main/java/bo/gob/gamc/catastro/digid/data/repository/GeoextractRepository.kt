package bo.gob.gamc.catastro.digid.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import bo.gob.gamc.catastro.digid.data.remote.api.GeoextractApi
import bo.gob.gamc.catastro.digid.data.remote.dto.GeoextractDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class GeoextractRepository @Inject constructor(
    private val geoextractApi: GeoextractApi
) {
    suspend fun listPendientes(): Result<List<GeoextractDto>> = withContext(Dispatchers.IO) {
        try {
            val res = geoextractApi.listCapturas()
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Error al listar pendientes ERP (${res.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadCaptura(imageFile: File): Result<GeoextractDto> = withContext(Dispatchers.IO) {
        try {
            val resizedFile = resizeImageIfNeeded(imageFile, maxWidth = 2400)
            val requestFile = resizedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", "captura.jpg", requestFile)

            val res = geoextractApi.uploadCaptura(part)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Error al subir foto a IDEC ERP: ${res.code()}"))
            }
        } catch (t: Throwable) {
            Result.failure(Exception(t.message ?: "Error al procesar o subir la captura"))
        }
    }

    private fun resizeImageIfNeeded(file: File, maxWidth: Int): File {
        return try {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight

            if (origWidth <= 0 || origHeight <= 0) return file
            if (origWidth <= maxWidth) return file

            var inSampleSize = 1
            while ((origWidth / inSampleSize) > (maxWidth * 2)) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val sampled = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return file

            val finalBitmap = if (sampled.width > maxWidth) {
                val ratio = maxWidth.toFloat() / sampled.width
                val newHeight = (sampled.height * ratio).roundToInt()
                val scaled = Bitmap.createScaledBitmap(sampled, maxWidth, newHeight, true)
                if (scaled != sampled) {
                    sampled.recycle()
                }
                scaled
            } else {
                sampled
            }

            val parentDir = file.parentFile ?: file
            val tempFile = File.createTempFile("geo_", ".jpg", parentDir)
            FileOutputStream(tempFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            finalBitmap.recycle()
            tempFile
        } catch (t: Throwable) {
            file
        }
    }
}
