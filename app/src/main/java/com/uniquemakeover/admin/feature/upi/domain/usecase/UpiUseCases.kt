package com.uniquemakeover.admin.feature.upi.domain.usecase

import javax.inject.Inject

data class UpiUseCases @Inject constructor(
    val buildUpiUri: BuildUpiUriUseCase,
    val generateQrMatrix: GenerateQrMatrixUseCase,
    val computePresetAmount: ComputePresetAmountUseCase,
    val buildWhatsAppMessage: BuildUpiWhatsAppMessageUseCase,
    val saveQrPng: SaveQrPngUseCase,
    val shareQr: ShareQrUseCase
)
