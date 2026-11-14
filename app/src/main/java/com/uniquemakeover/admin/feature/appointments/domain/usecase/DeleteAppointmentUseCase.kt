package com.uniquemakeover.admin.feature.appointments.domain.usecase

import com.uniquemakeover.admin.core.domain.repository.PaymentRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import javax.inject.Inject

class DeleteAppointmentUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val paymentRepository: PaymentRepository
) {

    suspend operator fun invoke(appointmentId: String): DataResult<Unit> {
        val paymentResult = paymentRepository.deletePaymentsByAppointmentId(appointmentId)
        return when (paymentResult) {
            is DataResult.Error -> paymentResult
            is DataResult.Success -> appointmentRepository.deleteById(appointmentId)
        }
    }
}
