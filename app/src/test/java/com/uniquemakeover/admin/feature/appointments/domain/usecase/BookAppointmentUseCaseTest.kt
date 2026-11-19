package com.uniquemakeover.admin.feature.appointments.domain.usecase

import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.core.domain.model.Payment
import com.uniquemakeover.admin.core.domain.model.PaymentMode
import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.domain.model.Slot
import com.uniquemakeover.admin.core.domain.repository.PaymentRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.PhoneSanitizer
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.model.BookingDraft
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.appointments.domain.repository.SlotRepository
import com.uniquemakeover.admin.testutil.FakeTimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BookAppointmentUseCaseTest {

    private val service1 = Service("svc_01", "Keratin Spa", 1500)

    private lateinit var appointmentRepository: FakeAppointmentRepository
    private lateinit var paymentRepository: FakePaymentRepository
    private lateinit var slotRepository: FakeSlotRepository
    private lateinit var phoneSanitizer: PhoneSanitizer
    private lateinit var validateBookingDraftUseCase: ValidateBookingDraftUseCase
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var bookAppointmentUseCase: BookAppointmentUseCase

    @Before
    fun setUp() {
        appointmentRepository = FakeAppointmentRepository()
        paymentRepository = FakePaymentRepository()
        slotRepository = FakeSlotRepository()
        phoneSanitizer = PhoneSanitizer()
        validateBookingDraftUseCase = ValidateBookingDraftUseCase(phoneSanitizer)
        timeProvider = FakeTimeProvider(
            currentTime24 = "10:00",
            currentDate = "2026-11-20",
            currentMillis = 1700000000000L
        )
        bookAppointmentUseCase = BookAppointmentUseCase(
            validateBookingDraftUseCase = validateBookingDraftUseCase,
            appointmentRepository = appointmentRepository,
            paymentRepository = paymentRepository,
            slotRepository = slotRepository,
            phoneSanitizer = phoneSanitizer,
            timeProvider = timeProvider
        )
    }

    @Test
    fun invoke_withAdvancePayment_insertsPaymentRecordInRepository() = runBlocking {
        val draft = BookingDraft(
            name = "Kavita Patil",
            phone = "9822112233",
            selectedServices = listOf(service1),
            date = "2026-11-25",
            time = "11:00",
            staff = "Sunita",
            advancePaid = 500
        )

        val result = bookAppointmentUseCase(draft)
        assertTrue(result is DataResult.Success)

        val appointmentId = (result as DataResult.Success).data
        val insertedAppointment = appointmentRepository.savedAppointments.firstOrNull { it.id == appointmentId }
        assertTrue(insertedAppointment != null)
        assertEquals(500, insertedAppointment?.paid)

        // Assert payment record was inserted in PaymentRepository
        assertEquals(1, paymentRepository.recordedPayments.size)
        val recordedPayment = paymentRepository.recordedPayments.first()
        assertEquals(appointmentId, recordedPayment.appointmentId)
        assertEquals(500, recordedPayment.amount)
        assertEquals(PaymentMode.CASH, recordedPayment.mode)
        assertEquals(1700000000000L, recordedPayment.paidAt)
    }

    @Test
    fun invoke_withZeroAdvancePayment_doesNotInsertPaymentRecord() = runBlocking {
        val draft = BookingDraft(
            name = "Kavita Patil",
            phone = "9822112233",
            selectedServices = listOf(service1),
            date = "2026-11-25",
            time = "11:00",
            staff = "Sunita",
            advancePaid = 0
        )

        val result = bookAppointmentUseCase(draft)
        assertTrue(result is DataResult.Success)

        val appointmentId = (result as DataResult.Success).data
        val insertedAppointment = appointmentRepository.savedAppointments.firstOrNull { it.id == appointmentId }
        assertTrue(insertedAppointment != null)
        assertEquals(0, insertedAppointment?.paid)

        // Assert zero payments recorded
        assertTrue(paymentRepository.recordedPayments.isEmpty())
    }

    private class FakeAppointmentRepository : AppointmentRepository {
        val savedAppointments = mutableListOf<Appointment>()

        override fun getAllAppointments(): Flow<List<Appointment>> = flowOf(savedAppointments)
        override fun getAppointmentsByDate(date: String): Flow<List<Appointment>> = emptyFlow()
        override suspend fun getAppointmentsByDateSync(date: String): List<Appointment> =
            savedAppointments.filter { it.date == date }

        override suspend fun getAppointmentById(id: String): Appointment? {
            return savedAppointments.firstOrNull { it.id == id }
        }

        override fun getTodaySchedule(today: String): Flow<List<Appointment>> = emptyFlow()
        override fun getFilteredAppointments(
            status: String?,
            startDate: String?,
            endDate: String?,
            query: String
        ): Flow<List<Appointment>> = flowOf(savedAppointments)

        override suspend fun insertAppointment(appointment: Appointment): DataResult<Unit> {
            savedAppointments.add(appointment)
            return DataResult.Success(Unit)
        }

        override suspend fun updateAppointment(appointment: Appointment): DataResult<Unit> {
            val index = savedAppointments.indexOfFirst { it.id == appointment.id }
            if (index >= 0) savedAppointments[index] = appointment
            return DataResult.Success(Unit)
        }

        override suspend fun updateStatus(id: String, status: AppointmentStatus): DataResult<Unit> =
            DataResult.Success(Unit)

        override suspend fun deleteAppointment(appointment: Appointment): DataResult<Unit> {
            savedAppointments.removeAll { it.id == appointment.id }
            return DataResult.Success(Unit)
        }

        override suspend fun deleteById(id: String): DataResult<Unit> {
            savedAppointments.removeAll { it.id == id }
            return DataResult.Success(Unit)
        }

        override suspend fun countUpcomingAppointmentsForService(serviceId: String, today: String): Int = 0
        override suspend fun count(): Int = savedAppointments.size
    }

    private class FakePaymentRepository : PaymentRepository {
        val recordedPayments = mutableListOf<Payment>()

        override suspend fun recordPayment(payment: Payment): DataResult<Unit> {
            recordedPayments.add(payment)
            return DataResult.Success(Unit)
        }

        override fun getPaymentsForAppointment(appointmentId: String): Flow<List<Payment>> = emptyFlow()
        override suspend fun getPaymentsForAppointmentSync(appointmentId: String): List<Payment> =
            recordedPayments.filter { it.appointmentId == appointmentId }

        override suspend fun deletePaymentsByAppointmentId(appointmentId: String): DataResult<Unit> {
            recordedPayments.removeAll { it.appointmentId == appointmentId }
            return DataResult.Success(Unit)
        }
    }

    private class FakeSlotRepository : SlotRepository {
        var clashFree = true
        override suspend fun isClashFree(date: String, time: String, excludeAppointmentId: String?): Boolean = clashFree
        override suspend fun getAvailableSlots(date: String): List<Slot> = emptyList()
    }
}
