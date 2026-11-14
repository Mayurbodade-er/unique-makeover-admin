package com.uniquemakeover.admin.feature.appointments.domain.usecase

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.PhoneSanitizer
import com.uniquemakeover.admin.core.util.TimeProvider
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.model.BookingDraft
import com.uniquemakeover.admin.feature.appointments.domain.model.BookingValidationResult
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.appointments.domain.repository.SlotRepository
import java.util.UUID
import javax.inject.Inject

class BookAppointmentUseCase @Inject constructor(
    private val validateBookingDraftUseCase: ValidateBookingDraftUseCase,
    private val appointmentRepository: AppointmentRepository,
    private val slotRepository: SlotRepository,
    private val phoneSanitizer: PhoneSanitizer,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(draft: BookingDraft): DataResult<String> {
        val validationResult = validateBookingDraftUseCase(draft)
        if (validationResult is BookingValidationResult.Invalid) {
            val firstError = validationResult.nameError
                ?: validationResult.phoneError
                ?: validationResult.serviceError
                ?: validationResult.slotError
                ?: validationResult.financialError
                ?: UiText.DynamicString("Invalid booking details")
            return DataResult.Error(firstError)
        }

        return if (!slotRepository.isClashFree(draft.date, draft.time)) {
            DataResult.Error(
                UiText.DynamicString("Slot was just booked by another client. Please select another time")
            )
        } else {
            createAndInsertAppointment(draft)
        }
    }

    private suspend fun createAndInsertAppointment(draft: BookingDraft): DataResult<String> {
        val sanitizedPhone = phoneSanitizer.sanitize(draft.phone) ?: draft.phone
        val appointmentId = UUID.randomUUID().toString()
        val serviceSnapshot = draft.selectedServices.joinToString(" + ") { it.name }
        val serviceIds = draft.selectedServices.map { it.id }

        val appointment = Appointment(
            id = appointmentId,
            name = draft.name.trim(),
            phone = sanitizedPhone,
            serviceSnapshot = serviceSnapshot,
            serviceIds = serviceIds,
            rate = draft.finalRate,
            paid = draft.advancePaid,
            staff = draft.staff.ifBlank { "Sunita" },
            date = draft.date,
            time = draft.time,
            note = draft.internalNote.trim(),
            status = "booked",
            reminded = null,
            bookedAt = timeProvider.currentTimeMillis()
        )

        return when (val insertResult = appointmentRepository.insertAppointment(appointment)) {
            is DataResult.Success -> DataResult.Success(appointmentId)
            is DataResult.Error -> DataResult.Error(insertResult.message)
        }
    }
}
