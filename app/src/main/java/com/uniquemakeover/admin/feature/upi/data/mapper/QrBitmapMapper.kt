package com.uniquemakeover.admin.feature.upi.data.mapper

import android.graphics.Bitmap
import com.uniquemakeover.admin.feature.upi.domain.model.QrCodeMatrix

private const val COLOR_BLACK = 0xFF000000.toInt()
private const val COLOR_WHITE = 0xFFFFFFFF.toInt()

fun QrCodeMatrix.toBitmap(): Bitmap {
    val totalPixels = width * height
    val pixels = IntArray(totalPixels)
    for (i in 0 until totalPixels) {
        pixels[i] = if (bits[i]) COLOR_BLACK else COLOR_WHITE
    }
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}
