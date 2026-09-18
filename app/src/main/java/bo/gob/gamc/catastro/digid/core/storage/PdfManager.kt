package bo.gob.gamc.catastro.digid.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class PdfManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageManager: UserStorageManager
) {
    // Dimensiones estándar A4 en puntos (72 DPI)
    private val a4Width = 595
    private val a4Height = 842
    private val margin = 20

    suspend fun createPdfFromImages(imageFiles: List<File>, outputName: String = "documento"): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()

        try {
            imageFiles.forEachIndexed { index, imageFile ->
                val bitmap = decodeSampledBitmap(imageFile.absolutePath, a4Width * 2, a4Height * 2)
                    ?: return@forEachIndexed

                val pageInfo = PdfDocument.PageInfo.Builder(a4Width, a4Height, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val maxWidth = a4Width - margin * 2
                val maxHeight = a4Height - margin * 2

                val scale = min(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height).coerceAtMost(1f)
                val scaledWidth = bitmap.width * scale
                val scaledHeight = bitmap.height * scale

                val x = (a4Width - scaledWidth) / 2f
                val y = (a4Height - scaledHeight) / 2f

                canvas.drawBitmap(bitmap, null, android.graphics.RectF(x, y, x + scaledWidth, y + scaledHeight), Paint(Paint.FILTER_BITMAP_FLAG))
                pdfDocument.finishPage(page)
                bitmap.recycle()
            }

            val targetFile = File(storageManager.getUserDirectory(), "${outputName}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(targetFile).use { out ->
                pdfDocument.writeTo(out)
            }
            targetFile
        } finally {
            pdfDocument.close()
        }
    }

    private fun decodeSampledBitmap(filePath: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(filePath, options)

        var inSampleSize = 1
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inJustDecodeBounds = false
        }
        return BitmapFactory.decodeFile(filePath, decodeOptions)
    }
}
