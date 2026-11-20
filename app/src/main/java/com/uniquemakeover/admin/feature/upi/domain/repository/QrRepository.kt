package com.uniquemakeover.admin.feature.upi.domain.repository

import com.uniquemakeover.admin.feature.upi.domain.model.QrCodeMatrix

interface QrRepository {
    suspend fun generateQrMatrix(payload: String, sizePx: Int = 512): QrCodeMatrix
}
