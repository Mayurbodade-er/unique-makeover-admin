package com.uniquemakeover.admin.feature.upi.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.upi.data.platform.QrImageExporterImpl
import com.uniquemakeover.admin.feature.upi.domain.model.QrCodeMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileInputStream

@RunWith(RobolectricTestRunner::class)
class QrImageExporterTest {

    private lateinit var context: Context
    private lateinit var exporter: QrImageExporterImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        exporter = QrImageExporterImpl(
            context = context,
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun saveToGallery_generatesValidPngFile() = runTest {
        val matrix = QrCodeMatrix(32, 32, BooleanArray(32 * 32) { it % 2 == 0 })
        val result = exporter.saveToGallery(matrix, "TestQrExport")

        assertTrue("Expected Success but got $result", result is DataResult.Success)
    }

    @Test
    fun shareQr_cachesValidPngInCacheDirectory() = runTest {
        val matrix = QrCodeMatrix(32, 32, BooleanArray(32 * 32) { it % 3 == 0 })
        val result = exporter.shareQr(matrix, "Test Share QR")

        assertTrue("Expected Success but got $result", result is DataResult.Success)

        val cachedFile = File(context.cacheDir, "qr_share/qr_code.png")
        assertTrue("Cached PNG file must exist", cachedFile.exists())
        assertTrue("Cached PNG file must have size > 0", cachedFile.length() > 0)

        // Verify PNG magic bytes: 0x89 0x50 0x4E 0x47
        val bytes = ByteArray(4)
        FileInputStream(cachedFile).use { it.read(bytes) }
        assertEquals(0x89.toByte(), bytes[0])
        assertEquals(0x50.toByte(), bytes[1])
        assertEquals(0x4E.toByte(), bytes[2])
        assertEquals(0x47.toByte(), bytes[3])
    }
}
