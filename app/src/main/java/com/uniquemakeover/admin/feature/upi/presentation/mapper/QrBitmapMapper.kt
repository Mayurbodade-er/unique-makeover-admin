package com.uniquemakeover.admin.feature.upi.presentation.mapper

import android.graphics.Bitmap
import com.uniquemakeover.admin.feature.upi.domain.model.QrCodeMatrix

fun QrCodeMatrix.toBitmap(): Bitmap {
    val totalPixels = width * height
    val pixels = IntArray(totalPixels)
    val black = 0xFF000000.toInt()
    val white = 0xFFFFFFFF.toInt()
    for (i in 0 until totalPixels) {
        pixels[i] = if (bits[i]) black else white
    }
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}
