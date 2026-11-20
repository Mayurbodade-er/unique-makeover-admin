package com.uniquemakeover.admin.feature.upi.data

import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.uniquemakeover.admin.feature.upi.data.platform.ZxingQrEncoder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ZxingQrEncoderTest {

    private lateinit var encoder: ZxingQrEncoder

    @Before
    fun setUp() {
        encoder = ZxingQrEncoder()
    }

    @Test
    fun generateQrBitmap_returns512pxBitmap() = runTest {
        val payload = "upi://pay?pa=uniquemakeover@upi&pn=Unique%20Makeover&am=850.00&cu=INR"

        val bitmap = encoder.generateQrBitmap(payload, 512)

        assertNotNull(bitmap)
        assertEquals(512, bitmap.width)
        assertEquals(512, bitmap.height)
    }

    @Test
    fun generateQrMatrix_hasHighContrastPixels() = runTest {
        val payload = "upi://pay?pa=test@upi"

        val matrix = encoder.generateQrMatrix(payload, 512)

        var hasBlack = false
        var hasWhite = false
        for (i in 0 until (matrix.width * matrix.height)) {
            if (matrix.bits[i]) hasBlack = true else hasWhite = true
            if (hasBlack && hasWhite) break
        }

        assertTrue("QR code must have black modules", hasBlack)
        assertTrue("QR code must have white modules", hasWhite)
    }

    @Test
    fun roundTrip_decodeBitmapMatchesOriginalPayload() = runTest {
        val originalPayload = "upi://pay?pa=uniquemakeover@upi&pn=Unique%20Makeover" +
            "&am=850.00&cu=INR&tn=Pooja%20-%20Balance%20Payment"

        val bitmap = encoder.generateQrBitmap(originalPayload, 512)

        // Read pixels back into ZXing luminance source
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader()
        val result = reader.decode(binaryBitmap)

        assertEquals("Decoded QR string must exactly match encoded payload", originalPayload, result.text)
    }
}
