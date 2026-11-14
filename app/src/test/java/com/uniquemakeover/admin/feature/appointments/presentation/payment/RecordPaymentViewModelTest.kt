package com.uniquemakeover.admin.feature.appointments.presentation.payment

import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.core.domain.model.Payment
import com.uniquemakeover.admin.core.domain.model.PaymentMode
import com.uniquemakeover.admin.core.domain.repository.PaymentRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.appointments.domain.usecase.RecordPaymentUseCase
import com.uniquemakeover.admin.testutil.FakeTimeProvider
import com.uniquemakeover.admin.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordPaymentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeAppointmentRepository: FakeAppointmentRepository
    private lateinit var fakePaymentRepository: FakePaymentRepository
    private lateinit var recordPaymentUseCase: RecordPaymentUseCase
    private lateinit var viewModel: RecordPaymentViewModel

    private val sampleAppointment = Appointment(
        id = "apt_vm_1",
        name = "Meera Deshmukh",
        phone = "9822334455",
        serviceSnapshot = "Facial",
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
        fakeAppointmentRepository = FakeAppointmentRepository(mutableMapOf(sampleAppointment.id to sampleAppointment))
        fakePaymentRepository = FakePaymentRepository()
        recordPaymentUseCase = RecordPaymentUseCase(
            appointmentRepository = fakeAppointmentRepository,
            paymentRepository = fakePaymentRepository,
            timeProvider = fakeTimeProvider
        )
        viewModel = RecordPaymentViewModel(
            recordPaymentUseCase = recordPaymentUseCase,
            appointmentRepository = fakeAppointmentRepository
        )
    }

    @Test
    fun loadAppointment_prefillsRemainingBalanceDue() {
        viewModel.loadAppointment(sampleAppointment)
        val state = viewModel.uiState.value

        assertEquals(1000, state.balanceDue)
        assertEquals("1000", state.amountInput)
        assertEquals(PaymentMode.CASH, state.mode)
        assertFalse(state.isSaved)
    }

    @Test
    fun onAmountChanged_filtersNonDigits() {
        viewModel.loadAppointment(sampleAppointment)
        viewModel.onAmountChanged("₹ 750abc")

        assertEquals("750", viewModel.uiState.value.amountInput)
    }

    @Test
    fun recordPayment_validAmount_setsIsSavedTrue() = runTest {
        viewModel.loadAppointment(sampleAppointment)
        viewModel.onAmountChanged("1000")
        viewModel.onModeChanged(PaymentMode.UPI)

        viewModel.recordPayment()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSaved)
        assertFalse(state.isOverpaymentConfirmationVisible)
        assertEquals(1, fakePaymentRepository.storedPayments.size)
        assertEquals(PaymentMode.UPI, fakePaymentRepository.storedPayments[0].mode)
    }

    @Test
    fun recordPayment_overpaymentWithoutConfirmation_showsConfirmationPrompt() = runTest {
        viewModel.loadAppointment(sampleAppointment)
        viewModel.onAmountChanged("1500") // Due is 1000

        viewModel.recordPayment(confirmedOverpayment = false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaved)
        assertTrue(state.isOverpaymentConfirmationVisible)
        assertTrue(state.overpaymentPrompt.contains("exceeds balance due"))
    }

    @Test
    fun recordPayment_overpaymentConfirmed_setsIsSavedTrue() = runTest {
        viewModel.loadAppointment(sampleAppointment)
        viewModel.onAmountChanged("1500")

        viewModel.recordPayment(confirmedOverpayment = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSaved)
        assertFalse(state.isOverpaymentConfirmationVisible)
        assertEquals(1500, fakePaymentRepository.storedPayments[0].amount)
    }

    private class FakePaymentRepository : PaymentRepository {
        val storedPayments = mutableListOf<Payment>()
        override fun getPaymentsForAppointment(appointmentId: String): Flow<List<Payment>> = flowOf(storedPayments)
        override suspend fun getPaymentsForAppointmentSync(appointmentId: String): List<Payment> = storedPayments
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
        override fun getAppointmentsByDate(date: String): Flow<List<Appointment>> = flowOf(storage.values.toList())
        override suspend fun getAppointmentsByDateSync(date: String): List<Appointment> = storage.values.toList()
        override fun getTodaySchedule(today: String): Flow<List<Appointment>> = flowOf(storage.values.toList())
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
