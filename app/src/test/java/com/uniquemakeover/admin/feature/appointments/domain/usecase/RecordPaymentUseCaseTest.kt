package com.uniquemakeover.admin.feature.appointments.domain.usecase

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.core.domain.model.Payment
import com.uniquemakeover.admin.core.domain.model.PaymentMode
import com.uniquemakeover.admin.core.domain.repository.PaymentRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.testutil.FakeTimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecordPaymentUseCaseTest {

    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeAppointmentRepository: FakeAppointmentRepository
    private lateinit var fakePaymentRepository: FakePaymentRepository
    private lateinit var useCase: RecordPaymentUseCase

    private val seedAppointment = Appointment(
        id = "apt_pay_1",
        name = "Meera Deshmukh",
        phone = "9822334455",
        serviceSnapshot = "Hydra Facial",
        serviceIds = listOf("s1"),
        rate = 1500,
        paid = 500,
        staff = "Sunita",
        date = "2026-11-20",
        time = "14:00",
        note = "",
        status = "booked",
        bookedAt = 1000L
    )

    @Before
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeTimeProvider.currentMillis = 1700000000000L
        fakeAppointmentRepository = FakeAppointmentRepository(mutableMapOf(seedAppointment.id to seedAppointment))
        fakePaymentRepository = FakePaymentRepository()
        useCase = RecordPaymentUseCase(
            appointmentRepository = fakeAppointmentRepository,
            paymentRepository = fakePaymentRepository,
            timeProvider = fakeTimeProvider
        )
    }

    @Test
    fun recordPayment_validExactBalance_recordsSuccessfully() = runBlocking {
        val result = useCase(
            appointmentId = "apt_pay_1",
            amount = 1000,
            mode = PaymentMode.CASH
        )

        assertTrue(result is DataResult.Success)
        assertEquals(1, fakePaymentRepository.storedPayments.size)
        val payment = fakePaymentRepository.storedPayments[0]
        assertEquals("apt_pay_1", payment.appointmentId)
        assertEquals(1000, payment.amount)
        assertEquals(PaymentMode.CASH, payment.mode)
        assertEquals(1700000000000L, payment.paidAt)
    }

    @Test
    fun recordPayment_zeroOrNegativeAmount_returnsError() = runBlocking {
        val zeroResult = useCase("apt_pay_1", 0, PaymentMode.CASH)
        assertTrue(zeroResult is DataResult.Error)
        val zeroMsg = ((zeroResult as DataResult.Error).message as UiText.DynamicString).value
        assertEquals("Payment must be greater than ₹0", zeroMsg)

        val negResult = useCase("apt_pay_1", -500, PaymentMode.UPI)
        assertTrue(negResult is DataResult.Error)
        val negMsg = ((negResult as DataResult.Error).message as UiText.DynamicString).value
        assertEquals("Payment must be greater than ₹0", negMsg)
    }

    @Test
    fun recordPayment_overpaymentWithoutConfirmation_returnsConfirmationRequiredError() = runBlocking {
        // Balance due is 1500 - 500 = 1000. Try paying 1200 without confirmation.
        val result = useCase(
            appointmentId = "apt_pay_1",
            amount = 1200,
            mode = PaymentMode.UPI,
            confirmedOverpayment = false
        )

        assertTrue(result is DataResult.Error)
        val message = ((result as DataResult.Error).message as UiText.DynamicString).value
        assertTrue(message.contains("exceeds balance due"))
        assertTrue(message.contains("1200"))
        assertTrue(message.contains("1000"))
        assertEquals(0, fakePaymentRepository.storedPayments.size)
    }

    @Test
    fun recordPayment_overpaymentWithConfirmation_recordsSuccessfully() = runBlocking {
        // Balance due is 1000. Pay 1200 with confirmation.
        val result = useCase(
            appointmentId = "apt_pay_1",
            amount = 1200,
            mode = PaymentMode.UPI,
            confirmedOverpayment = true
        )

        assertTrue(result is DataResult.Success)
        assertEquals(1, fakePaymentRepository.storedPayments.size)
        assertEquals(1200, fakePaymentRepository.storedPayments[0].amount)
        assertEquals(PaymentMode.UPI, fakePaymentRepository.storedPayments[0].mode)
    }

    @Test
    fun recordPayment_appointmentNotFound_returnsError() = runBlocking {
        val result = useCase("unknown_id", 500, PaymentMode.CASH)
        assertTrue(result is DataResult.Error)
        val message = ((result as DataResult.Error).message as UiText.DynamicString).value
        assertEquals("Appointment not found", message)
    }

    private class FakePaymentRepository : PaymentRepository {
        val storedPayments = mutableListOf<Payment>()

        override fun getPaymentsForAppointment(appointmentId: String): Flow<List<Payment>> =
            flowOf(storedPayments.filter { it.appointmentId == appointmentId })

        override suspend fun getPaymentsForAppointmentSync(appointmentId: String): List<Payment> =
            storedPayments.filter { it.appointmentId == appointmentId }

        override suspend fun recordPayment(payment: Payment): DataResult<Unit> {
            storedPayments.add(payment)
            return DataResult.Success(Unit)
        }

        override suspend fun deletePaymentsByAppointmentId(appointmentId: String): DataResult<Unit> {
            storedPayments.removeAll { it.appointmentId == appointmentId }
            return DataResult.Success(Unit)
        }
    }

    private class FakeAppointmentRepository(
        private val storage: MutableMap<String, Appointment>
    ) : AppointmentRepository {
        override fun getAllAppointments(): Flow<List<Appointment>> = flowOf(storage.values.toList())
        override fun getAppointmentsByDate(date: String): Flow<List<Appointment>> =
            flowOf(storage.values.filter { it.date == date })
        override suspend fun getAppointmentsByDateSync(date: String): List<Appointment> =
            storage.values.filter { it.date == date }
        override fun getTodaySchedule(today: String): Flow<List<Appointment>> =
            flowOf(storage.values.filter { it.date == today })
        override fun getFilteredAppointments(
            status: String?,
            startDate: String?,
            endDate: String?,
            query: String
        ): Flow<List<Appointment>> = flowOf(storage.values.toList())

        override suspend fun insertAppointment(appointment: Appointment): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun updateAppointment(appointment: Appointment): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun updateStatus(id: String, status: AppointmentStatus): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun getAppointmentById(id: String): Appointment? = storage[id]
        override suspend fun deleteAppointment(appointment: Appointment): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun deleteById(id: String): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun count(): Int = storage.size
    }
}
