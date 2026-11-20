package com.uniquemakeover.admin.feature.upi.data.platform

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.di.IoDispatcher
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.upi.data.mapper.toBitmap
import com.uniquemakeover.admin.feature.upi.domain.model.QrCodeMatrix
import com.uniquemakeover.admin.feature.upi.domain.repository.QrImageExporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject

private const val PNG_QUALITY = 100
private const val MIME_TYPE_PNG = "image/png"
private const val RELATIVE_PICTURES_PATH = "Pictures/UniqueMakeover"
private const val SALON_FOLDER = "UniqueMakeover"
private const val CACHE_QR_SUBDIR = "qr_share"
private const val CACHE_QR_FILENAME = "qr_code.png"

class QrImageExporterImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : QrImageExporter {

    override suspend fun saveToGallery(
        matrix: QrCodeMatrix,
        filename: String
    ): DataResult<String> = withContext(ioDispatcher) {
        try {
            val bitmap = matrix.toBitmap()
            val savedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToGalleryApi29(bitmap, filename)
            } else {
                saveToGalleryLegacy(bitmap, filename)
            }
            if (savedUri != null) {
                DataResult.Success(savedUri.toString())
            } else {
                DataResult.Error(UiText.DynamicString("Failed to save QR to Gallery"))
            }
        } catch (e: IOException) {
            DataResult.Error(UiText.DynamicString("Failed to save QR: ${e.localizedMessage}"))
        } catch (e: SecurityException) {
            DataResult.Error(UiText.DynamicString("Failed to save QR: ${e.localizedMessage}"))
        } catch (e: IllegalArgumentException) {
            DataResult.Error(UiText.DynamicString("Failed to save QR: ${e.localizedMessage}"))
        }
    }

    private fun saveToGalleryApi29(bitmap: Bitmap, filename: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.png")
            put(MediaStore.Images.Media.MIME_TYPE, MIME_TYPE_PNG)
            put(MediaStore.Images.Media.RELATIVE_PATH, RELATIVE_PICTURES_PATH)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = context.contentResolver.insert(collection, values)
        val writeSuccess = if (uri != null) {
            writeBitmapToStream(bitmap) { context.contentResolver.openOutputStream(uri) }
        } else {
            false
        }
        return if (writeSuccess && uri != null) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            uri
        } else {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun saveToGalleryLegacy(bitmap: Bitmap, filename: String): Uri? {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val salonDir = File(picturesDir, SALON_FOLDER).apply { if (!exists()) mkdirs() }
        val file = File(salonDir, "$filename.png")

        val writeSuccess = writeBitmapToStream(bitmap) { FileOutputStream(file) }
        if (!writeSuccess) return null

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.png")
            put(MediaStore.Images.Media.MIME_TYPE, MIME_TYPE_PNG)
            put(MediaStore.Images.Media.DATA, file.absolutePath)
        }
        return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: Uri.fromFile(file)
    }

    private inline fun writeBitmapToStream(
        bitmap: Bitmap,
        streamProvider: () -> OutputStream?
    ): Boolean {
        return streamProvider()?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, stream)
        } ?: false
    }

    override suspend fun shareQr(
        matrix: QrCodeMatrix,
        title: String
    ): DataResult<Unit> = withContext(ioDispatcher) {
        try {
            val cacheDir = File(context.cacheDir, CACHE_QR_SUBDIR).apply {
                if (!exists()) mkdirs()
            }
            val file = File(cacheDir, CACHE_QR_FILENAME)
            val writeSuccess = writeBitmapToStream(matrix.toBitmap()) { FileOutputStream(file) }
            if (writeSuccess) {
                val authority = "${context.packageName}.fileprovider"
                val contentUri = FileProvider.getUriForFile(context, authority, file)

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = MIME_TYPE_PNG
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, title)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(shareIntent, title).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                DataResult.Success(Unit)
            } else {
                DataResult.Error(UiText.DynamicString("Failed to write cached QR for sharing"))
            }
        } catch (e: IOException) {
            DataResult.Error(UiText.DynamicString("Failed to share QR: ${e.localizedMessage}"))
        } catch (e: SecurityException) {
            DataResult.Error(UiText.DynamicString("Failed to share QR: ${e.localizedMessage}"))
        } catch (e: IllegalArgumentException) {
            DataResult.Error(UiText.DynamicString("Failed to share QR: ${e.localizedMessage}"))
        }
    }
}
