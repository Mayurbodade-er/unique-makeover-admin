package com.uniquemakeover.admin.feature.upi.data.platform

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.uniquemakeover.admin.feature.upi.domain.model.QrCodeMatrix
import com.uniquemakeover.admin.feature.upi.domain.repository.QrRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZxingQrEncoder @Inject constructor() : QrRepository {

    override suspend fun generateQrMatrix(payload: String, sizePx: Int): QrCodeMatrix =
        withContext(Dispatchers.Default) {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
            )
            val bitMatrix = QRCodeWriter().encode(
                payload,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bits = BooleanArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    bits[offset + x] = bitMatrix.get(x, y)
                }
            }
            QrCodeMatrix(width = width, height = height, bits = bits)
        }

    fun toBitmap(matrix: QrCodeMatrix): Bitmap {
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        for (i in 0 until (width * height)) {
            pixels[i] = if (matrix.bits[i]) black else white
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    suspend fun generateQrBitmap(payload: String, sizePx: Int = 512): Bitmap {
        val matrix = generateQrMatrix(payload, sizePx)
        return toBitmap(matrix)
    }
}
