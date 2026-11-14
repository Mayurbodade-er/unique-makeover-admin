package com.uniquemakeover.admin.feature.appointments.data.repository

import android.database.SQLException
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.database.dao.AppointmentDao
import com.uniquemakeover.admin.core.database.dao.PaymentDao
import com.uniquemakeover.admin.core.di.IoDispatcher
import com.uniquemakeover.admin.core.domain.model.Payment
import com.uniquemakeover.admin.core.domain.repository.PaymentRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.data.mapper.PaymentMapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PaymentRepositoryImpl @Inject constructor(
    private val paymentDao: PaymentDao,
    private val appointmentDao: AppointmentDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PaymentRepository {

    override fun getPaymentsForAppointment(appointmentId: String): Flow<List<Payment>> {
        return paymentDao.getPaymentsForAppointment(appointmentId)
            .map { list -> list.map(PaymentMapper::toDomain) }
    }

    override suspend fun getPaymentsForAppointmentSync(appointmentId: String): List<Payment> {
        return withContext(ioDispatcher) {
            paymentDao.getPaymentsForAppointmentSync(appointmentId).map(PaymentMapper::toDomain)
        }
    }

    override suspend fun recordPayment(payment: Payment): DataResult<Unit> {
        return withContext(ioDispatcher) {
            try {
                paymentDao.insert(PaymentMapper.toEntity(payment))
                appointmentDao.addPayment(payment.appointmentId, payment.amount)
                DataResult.Success(Unit)
            } catch (e: SQLException) {
                DataResult.Error(UiText.DynamicString(e.localizedMessage ?: "Failed to record payment"))
            }
        }
    }
}
