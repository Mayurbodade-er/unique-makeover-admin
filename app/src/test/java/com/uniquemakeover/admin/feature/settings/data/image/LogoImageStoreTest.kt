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
class LogoImageStoreTest {

    private lateinit var context: Context
    private lateinit var logoImageStore: LogoImageStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val compressor = ImageCompressorImpl(context, Dispatchers.Unconfined)
        logoImageStore = LogoImageStore(context, compressor, Dispatchers.Unconfined)
    }

    @Test
    fun saveLogo_centerCropsTo200x200Square() = runTest {
        // Create 400x300 rectangular source bitmap
        val sourceBitmap = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
        val tempSourceFile = File(context.cacheDir, "test_logo_source.png")
        FileOutputStream(tempSourceFile).use {
            sourceBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        val result = logoImageStore.saveLogo(Uri.fromFile(tempSourceFile).toString())
        assertTrue("Expected Success result", result is DataResult.Success)
        assertEquals("branding/logo.jpg", (result as DataResult.Success).data)

        val savedFile = File(context.filesDir, "branding/logo.jpg")
        assertTrue("Saved file must exist", savedFile.exists())

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(savedFile.absolutePath, options)
        assertEquals(200, options.outWidth)
        assertEquals(200, options.outHeight)

        // Test delete
        val deleteResult = logoImageStore.deleteLogo()
        assertTrue("Delete must succeed", deleteResult)
        assertFalse("File must be deleted", savedFile.exists())
    }
}
