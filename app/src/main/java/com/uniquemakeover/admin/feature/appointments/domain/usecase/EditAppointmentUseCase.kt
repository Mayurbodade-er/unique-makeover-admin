package com.uniquemakeover.admin.feature.appointments.domain.usecase

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.PhoneSanitizer
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.model.BookingDraft
import com.uniquemakeover.admin.feature.appointments.domain.model.BookingValidationResult
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.appointments.domain.repository.SlotRepository
import javax.inject.Inject

class EditAppointmentUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val slotRepository: SlotRepository,
    private val validateBookingDraftUseCase: ValidateBookingDraftUseCase,
    private val phoneSanitizer: PhoneSanitizer
) {

    suspend fun getAppointment(id: String): Appointment? {
        return appointmentRepository.getAppointmentById(id)
    }

    suspend operator fun invoke(
        appointmentId: String,
        updatedDraft: BookingDraft
    ): DataResult<Unit> {
        val existing = appointmentRepository.getAppointmentById(appointmentId)
            ?: return DataResult.Error(UiText.DynamicString("Appointment not found"))

        val validationError = validatePreconditions(existing, updatedDraft)
        return when {
            validationError != null -> DataResult.Error(validationError)
            else -> applyUpdate(existing, updatedDraft)
        }
    }

    private suspend fun validatePreconditions(
        existing: Appointment,
        draft: BookingDraft
    ): UiText? {
        val isLocked = existing.status.lowercase() in LOCKED_STATUSES
        val isDateTimeChanged = existing.date != draft.date || existing.time != draft.time
        val validationResult = validateBookingDraftUseCase(draft)
        val isClashFree = slotRepository.isClashFree(draft.date, draft.time, excludeId = existing.id)

        return when {
            isLocked && isDateTimeChanged ->
                UiText.DynamicString("To reschedule, first restore appointment status to Booked")
            validationResult is BookingValidationResult.Invalid ->
                validationResult.firstError()
            !isClashFree ->
                UiText.DynamicString("Slot was just booked by another client. Please select another time")
            else -> null
        }
    }

    private suspend fun applyUpdate(
        existing: Appointment,
        draft: BookingDraft
    ): DataResult<Unit> {
        val sanitizedPhone = phoneSanitizer.sanitize(draft.phone) ?: draft.phone
        val serviceSnapshot = draft.selectedServices.joinToString(" + ") { it.name }
        val serviceIds = draft.selectedServices.map { it.id }

        val updated = existing.copy(
            name = draft.name.trim(),
            phone = sanitizedPhone,
            serviceSnapshot = serviceSnapshot,
            serviceIds = serviceIds,
            rate = draft.finalRate,
            staff = draft.staff.ifBlank { "Sunita" },
            date = draft.date,
            time = draft.time,
            note = draft.internalNote.trim()
        )
        return appointmentRepository.updateAppointment(updated)
    }

    private fun BookingValidationResult.Invalid.firstError(): UiText {
        return nameError ?: phoneError ?: serviceError ?: slotError ?: financialError
            ?: UiText.DynamicString("Invalid booking details")
    }

    companion object {
        private val LOCKED_STATUSES = setOf("done", "completed", "cancel", "cancelled")
    }
}
