package com.uniquemakeover.admin.feature.upi.domain.usecase

import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.upi.domain.model.QrCodeMatrix
import com.uniquemakeover.admin.feature.upi.domain.repository.QrImageExporter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SaveQrPngUseCaseTest {

    private lateinit var fakeExporter: FakeQrImageExporter
    private lateinit var saveQrPngUseCase: SaveQrPngUseCase

    @Before
    fun setUp() {
        fakeExporter = FakeQrImageExporter()
        saveQrPngUseCase = SaveQrPngUseCase(fakeExporter)
    }

    @Test
    fun invoke_delegatesToExporter_andReturnsSuccess() = runTest {
        val matrix = QrCodeMatrix(10, 10, BooleanArray(100))
        val result = saveQrPngUseCase(matrix, "test_qr")

        assertTrue(result is DataResult.Success)
        assertEquals("content://media/external/images/media/1", (result as DataResult.Success).data)
        assertEquals("test_qr", fakeExporter.lastSavedFilename)
        assertEquals(matrix, fakeExporter.lastSavedMatrix)
    }

    private class FakeQrImageExporter : QrImageExporter {
        var lastSavedMatrix: QrCodeMatrix? = null
        var lastSavedFilename: String? = null

        override suspend fun saveToGallery(
            matrix: QrCodeMatrix,
            filename: String
        ): DataResult<String> {
            lastSavedMatrix = matrix
            lastSavedFilename = filename
            return DataResult.Success("content://media/external/images/media/1")
        }

        override suspend fun shareQr(matrix: QrCodeMatrix, title: String): DataResult<Unit> {
            return DataResult.Success(Unit)
        }
    }
}
