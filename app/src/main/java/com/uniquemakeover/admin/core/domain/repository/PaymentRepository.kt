package com.uniquemakeover.admin.core.domain.repository

import com.uniquemakeover.admin.core.domain.model.Payment
import com.uniquemakeover.admin.core.domain.result.DataResult
import kotlinx.coroutines.flow.Flow

interface PaymentRepository {
    fun getPaymentsForAppointment(appointmentId: String): Flow<List<Payment>>
    suspend fun getPaymentsForAppointmentSync(appointmentId: String): List<Payment>
    suspend fun recordPayment(payment: Payment): DataResult<Unit>
}
