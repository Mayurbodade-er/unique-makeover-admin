package com.uniquemakeover.admin.feature.appointments.domain.usecase

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import javax.inject.Inject

class CancelAppointmentUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository
) {
    suspend operator fun invoke(appointmentId: String): DataResult<Unit> {
        val appointment = appointmentRepository.getAppointmentById(appointmentId)
            ?: return DataResult.Error(UiText.DynamicString("Appointment not found"))

        val currentStatus = AppointmentStatus.fromValue(appointment.status)
        return when (currentStatus) {
            AppointmentStatus.DONE -> DataResult.Error(
                UiText.DynamicString("Cannot cancel a completed appointment directly")
            )
            AppointmentStatus.CANCEL -> DataResult.Success(Unit)
            AppointmentStatus.BOOKED -> appointmentRepository.updateStatus(
                appointmentId,
                AppointmentStatus.CANCEL
            )
        }
    }
}
