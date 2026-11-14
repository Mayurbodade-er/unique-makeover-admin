package com.uniquemakeover.admin.feature.appointments.domain.usecase

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.core.domain.model.Payment
import com.uniquemakeover.admin.core.domain.repository.PaymentRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeleteAppointmentUseCaseTest {

    private lateinit var fakeAppointmentRepository: FakeAppointmentRepository
    private lateinit var fakePaymentRepository: FakePaymentRepository
    private lateinit var deleteAppointmentUseCase: DeleteAppointmentUseCase

    @Before
    fun setUp() {
        fakeAppointmentRepository = FakeAppointmentRepository()
        fakePaymentRepository = FakePaymentRepository()
        deleteAppointmentUseCase = DeleteAppointmentUseCase(
            appointmentRepository = fakeAppointmentRepository,
            paymentRepository = fakePaymentRepository
        )
    }

    @Test
    fun deleteAppointment_success_purgesBothAppointmentAndPayments() = runBlocking {
        val targetId = "apt_1001"
        fakeAppointmentRepository.deletedIds.clear()
        fakePaymentRepository.deletedAppointmentIds.clear()

        val result = deleteAppointmentUseCase(targetId)

        assertTrue(result is DataResult.Success)
        assertEquals(listOf(targetId), fakePaymentRepository.deletedAppointmentIds)
        assertEquals(listOf(targetId), fakeAppointmentRepository.deletedIds)
    }

    @Test
    fun deleteAppointment_paymentFailure_abortsWithoutDeletingAppointment() = runBlocking {
        fakePaymentRepository.shouldFail = true
        val targetId = "apt_1002"

        val result = deleteAppointmentUseCase(targetId)

        assertTrue(result is DataResult.Error)
        val error = result as DataResult.Error
        assertEquals("Failed to delete payments", (error.message as UiText.DynamicString).value)
        assertTrue(fakeAppointmentRepository.deletedIds.isEmpty())
    }

    @Test
    fun deleteAppointment_appointmentFailure_returnsError() = runBlocking {
        fakeAppointmentRepository.shouldFail = true
        val targetId = "apt_1003"

        val result = deleteAppointmentUseCase(targetId)

        assertTrue(result is DataResult.Error)
        val error = result as DataResult.Error
        assertEquals("Failed to delete appointment", (error.message as UiText.DynamicString).value)
    }

    private class FakeAppointmentRepository : AppointmentRepository {
        val deletedIds = mutableListOf<String>()
        var shouldFail = false

        override fun getAllAppointments(): Flow<List<Appointment>> = emptyFlow()
        override fun getAppointmentsByDate(date: String): Flow<List<Appointment>> = emptyFlow()
        override suspend fun getAppointmentsByDateSync(date: String): List<Appointment> = emptyList()
        override suspend fun getAppointmentById(id: String): Appointment? = null
        override fun getTodaySchedule(today: String): Flow<List<Appointment>> = emptyFlow()
        override fun getFilteredAppointments(
            status: String?,
            startDate: String?,
            endDate: String?,
            query: String
        ): Flow<List<Appointment>> = emptyFlow()
        override suspend fun insertAppointment(appointment: Appointment): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun updateAppointment(appointment: Appointment): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun updateStatus(id: String, status: AppointmentStatus): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun deleteAppointment(appointment: Appointment): DataResult<Unit> = DataResult.Success(Unit)

        override suspend fun deleteById(id: String): DataResult<Unit> {
            return if (shouldFail) {
                DataResult.Error(UiText.DynamicString("Failed to delete appointment"))
            } else {
                deletedIds.add(id)
                DataResult.Success(Unit)
            }
        }

        override suspend fun count(): Int = 0
    }

    private class FakePaymentRepository : PaymentRepository {
        val deletedAppointmentIds = mutableListOf<String>()
        var shouldFail = false

        override fun getPaymentsForAppointment(appointmentId: String): Flow<List<Payment>> = emptyFlow()
        override suspend fun getPaymentsForAppointmentSync(appointmentId: String): List<Payment> = emptyList()
        override suspend fun recordPayment(payment: Payment): DataResult<Unit> = DataResult.Success(Unit)

        override suspend fun deletePaymentsByAppointmentId(appointmentId: String): DataResult<Unit> {
            return if (shouldFail) {
                DataResult.Error(UiText.DynamicString("Failed to delete payments"))
            } else {
                deletedAppointmentIds.add(appointmentId)
                DataResult.Success(Unit)
            }
        }
    }
}
