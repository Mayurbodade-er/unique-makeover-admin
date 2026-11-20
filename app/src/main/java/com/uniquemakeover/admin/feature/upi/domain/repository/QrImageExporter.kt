package com.uniquemakeover.admin.feature.upi.domain.repository

import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.upi.domain.model.QrCodeMatrix

interface QrImageExporter {
    suspend fun saveToGallery(matrix: QrCodeMatrix, filename: String): DataResult<String>
    suspend fun shareQr(matrix: QrCodeMatrix, title: String): DataResult<Unit>
}
