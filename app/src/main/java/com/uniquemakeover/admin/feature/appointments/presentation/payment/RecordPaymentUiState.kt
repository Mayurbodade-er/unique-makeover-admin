package com.uniquemakeover.admin.feature.appointments.presentation.payment

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.PaymentMode
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment

data class RecordPaymentUiState(
    val appointment: Appointment? = null,
    val amountInput: String = "",
    val mode: PaymentMode = PaymentMode.CASH,
    val isOverpaymentConfirmationVisible: Boolean = false,
    val overpaymentPrompt: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: UiText? = null
) {
    val balanceDue: Int
        get() = appointment?.let { (it.rate - it.paid).coerceAtLeast(0) } ?: 0
}
