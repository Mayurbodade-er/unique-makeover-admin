package com.uniquemakeover.admin.feature.upi.domain.usecase

import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.upi.domain.model.QrCodeMatrix
import com.uniquemakeover.admin.feature.upi.domain.repository.QrImageExporter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ShareQrUseCaseTest {

    private lateinit var fakeExporter: FakeQrImageExporter
    private lateinit var shareQrUseCase: ShareQrUseCase

    @Before
    fun setUp() {
        fakeExporter = FakeQrImageExporter()
        shareQrUseCase = ShareQrUseCase(fakeExporter)
    }

    @Test
    fun invoke_delegatesToExporter_andReturnsSuccess() = runTest {
        val matrix = QrCodeMatrix(10, 10, BooleanArray(100))
        val result = shareQrUseCase(matrix, "Unique Makeover QR")

        assertTrue(result is DataResult.Success)
        assertEquals("Unique Makeover QR", fakeExporter.lastShareTitle)
        assertEquals(matrix, fakeExporter.lastShareMatrix)
    }

    private class FakeQrImageExporter : QrImageExporter {
        var lastShareMatrix: QrCodeMatrix? = null
        var lastShareTitle: String? = null

        override suspend fun saveToGallery(
            matrix: QrCodeMatrix,
            filename: String
        ): DataResult<String> {
            return DataResult.Success("content://media/external/images/media/1")
        }

        override suspend fun shareQr(matrix: QrCodeMatrix, title: String): DataResult<Unit> {
            lastShareMatrix = matrix
            lastShareTitle = title
            return DataResult.Success(Unit)
        }
    }
}
