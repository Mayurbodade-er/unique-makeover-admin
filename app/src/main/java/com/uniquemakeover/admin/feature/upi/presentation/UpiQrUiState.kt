package com.uniquemakeover.admin.feature.upi.presentation

import android.graphics.Bitmap
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.feature.upi.domain.model.PaymentPreset

sealed interface UpiQrUiState {
    data object Loading : UpiQrUiState
    data object MissingConfiguration : UpiQrUiState

    data class Content(
        val qrBitmap: Bitmap? = null,
        val upiUri: String = "",
        val amount: Int = 0,
        val note: String = "",
        val selectedPreset: PaymentPreset = PaymentPreset.BalanceDue,
        val customAmountInput: String = "",
        val isAdvanceDisabled: Boolean = false,
        val shopName: String = "",
        val upiId: String = ""
    ) : UpiQrUiState

    data class Error(val message: UiText) : UpiQrUiState
}
