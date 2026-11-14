package com.uniquemakeover.admin.feature.services.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.ImageCompressorImpl
import com.uniquemakeover.admin.feature.services.data.image.LocalImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
class LocalImageStoreIntegrationTest {

    private lateinit var context: Context
    private lateinit var store: LocalImageStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val compressor = ImageCompressorImpl(context, Dispatchers.Unconfined)
        store = LocalImageStore(context, compressor, Dispatchers.Unconfined)
    }

    @Test
    fun saveServiceImage_and_deleteImage_operatesOnInternalStorage() = runTest {
        // Create a test file
        val sourceFile = File(context.cacheDir, "gallery_pick.jpg")
        val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        FileOutputStream(sourceFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        bitmap.recycle()

        val uriString = Uri.fromFile(sourceFile).toString()
        val saveResult = store.saveServiceImage(uriString)

        assertTrue(saveResult is DataResult.Success)
        val relativePath = (saveResult as DataResult.Success).data
        assertTrue(relativePath.startsWith("images/"))

        val storedFile = store.getAbsoluteFile(relativePath)
        assertTrue(storedFile.exists())

        // Delete test
        val deleteSuccess = store.deleteImage(relativePath)
        assertTrue(deleteSuccess)
        assertFalse(storedFile.exists())
    }
}
