package com.uniquemakeover.admin.feature.upi.domain.usecase

import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.upi.domain.model.QrCodeMatrix
import com.uniquemakeover.admin.feature.upi.domain.repository.QrImageExporter
import javax.inject.Inject

class SaveQrPngUseCase @Inject constructor(
    private val qrImageExporter: QrImageExporter
) {
    suspend operator fun invoke(
        matrix: QrCodeMatrix,
        filename: String = "UPI_QR_UniqueMakeover"
    ): DataResult<String> = qrImageExporter.saveToGallery(matrix, filename)
}
