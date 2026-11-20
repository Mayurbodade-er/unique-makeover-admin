package com.uniquemakeover.admin.feature.upi.domain.usecase

import com.uniquemakeover.admin.feature.upi.domain.model.QrCodeMatrix
import com.uniquemakeover.admin.feature.upi.domain.repository.QrRepository
import javax.inject.Inject

class GenerateQrMatrixUseCase @Inject constructor(
    private val qrRepository: QrRepository
) {
    suspend operator fun invoke(payload: String, sizePx: Int = 512): QrCodeMatrix =
        qrRepository.generateQrMatrix(payload, sizePx)
}
