package com.uniquemakeover.admin.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.uniquemakeover.admin.core.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val SERVICE_PHOTO_MAX_EDGE = 320
private const val SERVICE_PHOTO_JPEG_QUALITY = 78
private const val LOGO_MAX_EDGE = 200
private const val LOGO_JPEG_QUALITY = 85
private const val WALLPAPER_MAX_EDGE = 900
private const val WALLPAPER_JPEG_QUALITY = 72

@Singleton
class ImageCompressorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ImageCompressor {

    override suspend fun compressServicePhoto(sourceUri: Uri, destinationFile: File): File {
        return compressImage(
            sourceUri = sourceUri,
            destinationFile = destinationFile,
            targetMaxEdge = SERVICE_PHOTO_MAX_EDGE,
            quality = SERVICE_PHOTO_JPEG_QUALITY
        )
    }

    override suspend fun compressLogo(sourceUri: Uri, destinationFile: File): File {
        return compressImage(
            sourceUri = sourceUri,
            destinationFile = destinationFile,
            targetMaxEdge = LOGO_MAX_EDGE,
            quality = LOGO_JPEG_QUALITY,
            cropToSquare = true
        )
    }

    override suspend fun compressWallpaper(sourceUri: Uri, destinationFile: File): File {
        return compressImage(
            sourceUri = sourceUri,
            destinationFile = destinationFile,
            targetMaxEdge = WALLPAPER_MAX_EDGE,
            quality = WALLPAPER_JPEG_QUALITY
        )
    }

    private suspend fun compressImage(
        sourceUri: Uri,
        destinationFile: File,
        targetMaxEdge: Int,
        quality: Int,
        cropToSquare: Boolean = false
    ): File = withContext(ioDispatcher) {
        val bounds = decodeImageBounds(sourceUri)
        val inSampleSize = calculateInSampleSize(
            width = bounds.first,
            height = bounds.second,
            targetMaxEdge = targetMaxEdge,
            cropToSquare = cropToSquare
        )
        val decodedBitmap = decodeSampledBitmap(sourceUri, inSampleSize)
            ?: throw IllegalArgumentException("Unable to decode image from uri: $sourceUri")

        val preparedBitmap = if (cropToSquare) {
            cropToCenterSquare(decodedBitmap)
        } else {
            decodedBitmap
        }

        val scaledBitmap = scaleBitmapToMaxEdge(preparedBitmap, targetMaxEdge)
        if (scaledBitmap != preparedBitmap && preparedBitmap != decodedBitmap) {
            preparedBitmap.recycle()
        }
        if (preparedBitmap != decodedBitmap) {
            decodedBitmap.recycle()
        } else if (scaledBitmap != decodedBitmap) {
            decodedBitmap.recycle()
        }

        destinationFile.parentFile?.mkdirs()
        try {
            FileOutputStream(destinationFile).use { outStream ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outStream)
                outStream.flush()
            }
        } catch (e: IOException) {
            if (destinationFile.exists()) destinationFile.delete()
            throw IOException("Storage full. Free up device memory to save photos", e)
        } finally {
            scaledBitmap.recycle()
        }

        destinationFile
    }

    private fun cropToCenterSquare(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        if (width == height) return source
        val size = minOf(width, height)
        val x = (width - size) / 2
        val y = (height - size) / 2
        return Bitmap.createBitmap(source, x, y, size, size)
    }

    private fun decodeImageBounds(sourceUri: Uri): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val stream = context.contentResolver.openInputStream(sourceUri)
        requireNotNull(stream) { "Cannot open stream for URI: $sourceUri" }
        stream.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }

        require(options.outWidth > 0 && options.outHeight > 0) {
            "Invalid image dimensions from URI: $sourceUri"
        }
        return Pair(options.outWidth, options.outHeight)
    }

    private fun decodeSampledBitmap(sourceUri: Uri, inSampleSize: Int): Bitmap? {
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        }
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        targetMaxEdge: Int,
        cropToSquare: Boolean = false
    ): Int {
        var inSampleSize = 1
        val edgeToConsider = if (cropToSquare) minOf(width, height) else maxOf(width, height)
        while (edgeToConsider / (inSampleSize * 2) >= targetMaxEdge) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    private fun scaleBitmapToMaxEdge(source: Bitmap, targetMaxEdge: Int): Bitmap {
        val longestEdge = maxOf(source.width, source.height)
        if (longestEdge <= targetMaxEdge) {
            return source
        }
        val scale = targetMaxEdge.toFloat() / longestEdge
        val targetWidth = (source.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (source.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }
}
