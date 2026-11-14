package com.uniquemakeover.admin.feature.appointments.domain.usecase

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.Payment
import com.uniquemakeover.admin.core.domain.model.PaymentMode
import com.uniquemakeover.admin.core.domain.repository.PaymentRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.TimeProvider
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import java.util.UUID
import javax.inject.Inject

class RecordPaymentUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val paymentRepository: PaymentRepository,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(
        appointmentId: String,
        amount: Int,
        mode: PaymentMode,
        confirmedOverpayment: Boolean = false
    ): DataResult<Unit> {
        val validationError = validateAmount(amount)
        if (validationError != null) return validationError

        return processPayment(appointmentId, amount, mode, confirmedOverpayment)
    }

    private suspend fun processPayment(
        appointmentId: String,
        amount: Int,
        mode: PaymentMode,
        confirmedOverpayment: Boolean
    ): DataResult<Unit> {
        val appointment = appointmentRepository.getAppointmentById(appointmentId)
            ?: return DataResult.Error(UiText.DynamicString("Appointment not found"))

        val due = (appointment.rate - appointment.paid).coerceAtLeast(0)
        val overpaymentError = checkOverpayment(amount, due, confirmedOverpayment)
        return overpaymentError ?: savePayment(appointmentId, amount, mode)
    }

    private fun validateAmount(amount: Int): DataResult.Error? {
        if (amount <= 0) {
            return DataResult.Error(UiText.DynamicString("Payment must be greater than ₹0"))
        }
        return null
    }

    private fun checkOverpayment(amount: Int, balanceDue: Int, confirmed: Boolean): DataResult.Error? {
        if (amount > balanceDue && !confirmed) {
            return DataResult.Error(
                UiText.DynamicString(
                    "Amount entered (₹$amount) exceeds balance due (₹$balanceDue). Confirm extra payment/tip?"
                )
            )
        }
        return null
    }

    private suspend fun savePayment(
        appointmentId: String,
        amount: Int,
        mode: PaymentMode
    ): DataResult<Unit> {
        val payment = Payment(
            id = UUID.randomUUID().toString(),
            appointmentId = appointmentId,
            amount = amount,
            mode = mode,
            paidAt = timeProvider.currentTimeMillis()
        )
        return paymentRepository.recordPayment(payment)
    }
}
