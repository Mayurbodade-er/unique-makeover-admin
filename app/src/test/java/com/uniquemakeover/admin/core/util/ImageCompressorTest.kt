package com.uniquemakeover.admin.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileOutputStream

private const val MAX_SERVICE_PHOTO_EDGE = 320
private const val MAX_SERVICE_PHOTO_BYTES = 50 * 1024L // 50 KB

@RunWith(RobolectricTestRunner::class)
class ImageCompressorTest {

    private lateinit var context: Context
    private lateinit var compressor: ImageCompressorImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        compressor = ImageCompressorImpl(context, Dispatchers.Unconfined)
    }

    @Test
    fun compressServicePhoto_highResBitmap_downscalesBelow320pxAndUnder50KB() = runTest {
        // Create simulated 4000x3000 test bitmap
        val testFile = File(context.cacheDir, "test_large.jpg")
        val bitmap = Bitmap.createBitmap(4000, 3000, Bitmap.Config.ARGB_8888)
        FileOutputStream(testFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bitmap.recycle()

        val destFile = File(context.filesDir, "images/compressed_test.jpg")
        val sourceUri = Uri.fromFile(testFile)

        val result = compressor.compressServicePhoto(sourceUri, destFile)
        assertTrue(result.exists())
        assertTrue(result.length() < MAX_SERVICE_PHOTO_BYTES)

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(result.absolutePath, options)

        val longestEdge = maxOf(options.outWidth, options.outHeight)
        assertTrue("Longest edge should be <= 320, was $longestEdge", longestEdge <= MAX_SERVICE_PHOTO_EDGE)
    }

    @Test
    fun compressLogo_downscalesBelow200px() = runTest {
        val testFile = File(context.cacheDir, "test_logo.jpg")
        val bitmap = Bitmap.createBitmap(1200, 1200, Bitmap.Config.ARGB_8888)
        FileOutputStream(testFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bitmap.recycle()

        val destFile = File(context.filesDir, "images/logo_test.jpg")
        val result = compressor.compressLogo(Uri.fromFile(testFile), destFile)
        assertTrue(result.exists())

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(result.absolutePath, options)
        assertTrue(options.outWidth <= 200 && options.outHeight <= 200)
    }
}
