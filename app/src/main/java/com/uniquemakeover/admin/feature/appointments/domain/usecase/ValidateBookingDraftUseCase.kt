package com.uniquemakeover.admin.feature.appointments.domain.usecase

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.util.PhoneSanitizer
import com.uniquemakeover.admin.feature.appointments.domain.model.BookingDraft
import com.uniquemakeover.admin.feature.appointments.domain.model.BookingValidationResult
import javax.inject.Inject

class ValidateBookingDraftUseCase @Inject constructor(
    private val phoneSanitizer: PhoneSanitizer
) {
    operator fun invoke(draft: BookingDraft): BookingValidationResult {
        val nameError = if (draft.name.trim().isBlank()) {
            UiText.DynamicString("Client name is required")
        } else {
            null
        }

        val phoneError = if (!phoneSanitizer.isValid(draft.phone)) {
            UiText.DynamicString("Enter valid 10-digit Indian mobile number (starts with 6, 7, 8, or 9)")
        } else {
            null
        }

        val serviceError = if (draft.selectedServices.isEmpty()) {
            UiText.DynamicString("Select at least one service")
        } else {
            null
        }

        val slotError = if (draft.date.isBlank() || draft.time.isBlank()) {
            UiText.DynamicString("Select appointment date and time slot")
        } else {
            null
        }

        val financialError = validateFinancials(draft)

        val hasError = nameError != null || phoneError != null || serviceError != null ||
            slotError != null || financialError != null

        return if (hasError) {
            BookingValidationResult.Invalid(
                nameError = nameError,
                phoneError = phoneError,
                serviceError = serviceError,
                slotError = slotError,
                financialError = financialError
            )
        } else {
            BookingValidationResult.Valid
        }
    }

    private fun validateFinancials(draft: BookingDraft): UiText? {
        val subtotal = draft.subtotal
        val finalRate = draft.finalRate
        return when {
            draft.discount < 0 -> UiText.DynamicString("Discount cannot be negative")
            draft.discount > subtotal -> UiText.DynamicString("Discount cannot exceed subtotal (₹$subtotal)")
            draft.advancePaid < 0 -> UiText.DynamicString("Advance cannot be negative")
            draft.advancePaid > finalRate -> {
                UiText.DynamicString("Advance cannot exceed total bill amount (₹$finalRate)")
            }
            else -> null
        }
    }
}
