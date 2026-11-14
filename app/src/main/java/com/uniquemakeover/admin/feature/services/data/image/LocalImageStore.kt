package com.uniquemakeover.admin.feature.services.data.image

import android.content.Context
import android.net.Uri
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.di.IoDispatcher
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.ImageCompressor
import com.uniquemakeover.admin.feature.services.domain.repository.ImageStorageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageCompressor: ImageCompressor,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ImageStorageRepository {

    private val imagesDir: File
        get() = File(context.filesDir, "images").apply { if (!exists()) mkdirs() }

    override suspend fun saveServiceImage(sourceUriString: String): DataResult<String> = withContext(ioDispatcher) {
        try {
            val uri = Uri.parse(sourceUriString)
            val fileName = "${UUID.randomUUID()}.jpg"
            val destFile = File(imagesDir, fileName)
            imageCompressor.compressServicePhoto(uri, destFile)
            DataResult.Success("images/$fileName")
        } catch (_: IllegalArgumentException) {
            DataResult.Error(UiText.DynamicString("Unable to load selected image"))
        } catch (_: IOException) {
            DataResult.Error(UiText.DynamicString("Storage full. Free up device memory to save photos"))
        } catch (e: SecurityException) {
            DataResult.Error(UiText.DynamicString(e.localizedMessage ?: "Permission denied to read image"))
        }
    }

    override suspend fun deleteImage(relativePath: String): Boolean = withContext(ioDispatcher) {
        try {
            val file = File(context.filesDir, relativePath)
            if (file.exists()) file.delete() else true
        } catch (_: SecurityException) {
            false
        }
    }

    override fun getAbsoluteImagePath(relativePath: String): String {
        return File(context.filesDir, relativePath).absolutePath
    }

    fun getAbsoluteFile(relativePath: String): File {
        return File(context.filesDir, relativePath)
    }
}
