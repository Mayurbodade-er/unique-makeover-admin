package com.uniquemakeover.admin.feature.appointments.domain.usecase

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.core.domain.model.StatusUpdateResult
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.appointments.domain.repository.SlotRepository
import javax.inject.Inject

class UndoAppointmentStatusUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val slotRepository: SlotRepository
) {
    suspend operator fun invoke(
        appointmentId: String,
        previousStatus: AppointmentStatus
    ): StatusUpdateResult {
        val appointment = appointmentRepository.getAppointmentById(appointmentId)
            ?: return StatusUpdateResult.NotFound(UiText.DynamicString("Appointment not found"))

        val currentStatus = AppointmentStatus.fromValue(appointment.status)
        return executeUndo(appointment, currentStatus, previousStatus)
    }

    private suspend fun executeUndo(
        appointment: Appointment,
        currentStatus: AppointmentStatus,
        previousStatus: AppointmentStatus
    ): StatusUpdateResult {
        if (currentStatus == previousStatus) {
            return StatusUpdateResult.Success
        }

        val conflict = checkClashConflict(appointment, currentStatus, previousStatus)
        return conflict ?: applyRevert(appointment.id, previousStatus)
    }

    private suspend fun checkClashConflict(
        appointment: Appointment,
        currentStatus: AppointmentStatus,
        previousStatus: AppointmentStatus
    ): StatusUpdateResult? {
        val needsCheck = currentStatus == AppointmentStatus.CANCEL && previousStatus == AppointmentStatus.BOOKED
        if (needsCheck && !slotRepository.isClashFree(appointment.date, appointment.time, appointment.id)) {
            return StatusUpdateResult.SlotConflict(
                UiText.DynamicString("Slot is now occupied by another client. Please reschedule.")
            )
        }
        return null
    }

    private suspend fun applyRevert(
        appointmentId: String,
        previousStatus: AppointmentStatus
    ): StatusUpdateResult {
        return when (val res = appointmentRepository.updateStatus(appointmentId, previousStatus)) {
            is DataResult.Success -> StatusUpdateResult.Success
            is DataResult.Error -> StatusUpdateResult.NotFound(res.message)
        }
    }
}
