package com.uniquemakeover.admin.feature.settings.data.image

import android.content.Context
import android.net.Uri
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.di.IoDispatcher
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.ImageCompressor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val BRANDING_SUBDIR = "branding"
private const val WALLPAPER_FILENAME = "wallpaper.jpg"
private const val WALLPAPER_RELATIVE_PATH = "branding/wallpaper.jpg"

@Singleton
class WallpaperImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageCompressor: ImageCompressor,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val brandingDir: File
        get() = File(context.filesDir, BRANDING_SUBDIR).apply { if (!exists()) mkdirs() }

    suspend fun saveWallpaper(sourceUriString: String): DataResult<String> = withContext(ioDispatcher) {
        try {
            val uri = Uri.parse(sourceUriString)
            val destFile = File(brandingDir, WALLPAPER_FILENAME)
            imageCompressor.compressWallpaper(uri, destFile)
            DataResult.Success(WALLPAPER_RELATIVE_PATH)
        } catch (_: IllegalArgumentException) {
            DataResult.Error(UiText.DynamicString("Invalid image file. Please choose another"))
        } catch (_: IOException) {
            DataResult.Error(UiText.DynamicString("Storage full. Free up device memory to save photos"))
        } catch (e: SecurityException) {
            DataResult.Error(UiText.DynamicString(e.localizedMessage ?: "Permission denied to read image"))
        }
    }

    suspend fun deleteWallpaper(): Boolean = withContext(ioDispatcher) {
        try {
            val file = File(context.filesDir, WALLPAPER_RELATIVE_PATH)
            if (file.exists()) file.delete() else true
        } catch (_: SecurityException) {
            false
        }
    }
}
