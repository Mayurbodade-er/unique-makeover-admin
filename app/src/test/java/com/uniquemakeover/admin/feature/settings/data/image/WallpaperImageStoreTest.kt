package com.uniquemakeover.admin.feature.settings.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.ImageCompressorImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
class WallpaperImageStoreTest {

    private lateinit var context: Context
    private lateinit var wallpaperImageStore: WallpaperImageStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val compressor = ImageCompressorImpl(context, Dispatchers.Unconfined)
        wallpaperImageStore = WallpaperImageStore(context, compressor, Dispatchers.Unconfined)
    }

    @Test
    fun saveWallpaper_scalesTo900pxMaxEdge() = runTest {
        // Create 1800x1200 bitmap
        val sourceBitmap = Bitmap.createBitmap(1800, 1200, Bitmap.Config.ARGB_8888)
        val tempSourceFile = File(context.cacheDir, "test_wallpaper_source.png")
        FileOutputStream(tempSourceFile).use {
            sourceBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        val result = wallpaperImageStore.saveWallpaper(Uri.fromFile(tempSourceFile).toString())
        assertTrue("Expected Success result", result is DataResult.Success)
        assertEquals("branding/wallpaper.jpg", (result as DataResult.Success).data)

        val savedFile = File(context.filesDir, "branding/wallpaper.jpg")
        assertTrue("Saved file must exist", savedFile.exists())

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(savedFile.absolutePath, options)
        val maxEdge = maxOf(options.outWidth, options.outHeight)
        assertTrue("Max edge should be <= 900 but was $maxEdge", maxEdge <= 900)

        // Test delete
        val deleteResult = wallpaperImageStore.deleteWallpaper()
        assertTrue("Delete must succeed", deleteResult)
        assertFalse("File must be deleted", savedFile.exists())
    }
}
