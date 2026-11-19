package com.uniquemakeover.admin.feature.appointments.presentation.booking

import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.core.domain.model.Payment
import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.core.domain.model.Slot
import com.uniquemakeover.admin.core.domain.repository.PaymentRepository
import com.uniquemakeover.admin.core.domain.repository.ServiceRepository
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.PhoneSanitizer
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.appointments.domain.repository.SlotRepository
import com.uniquemakeover.admin.feature.appointments.domain.usecase.BookAppointmentUseCase
import com.uniquemakeover.admin.feature.appointments.domain.usecase.EditAppointmentUseCase
import com.uniquemakeover.admin.feature.appointments.domain.usecase.GenerateAvailableSlotsUseCase
import com.uniquemakeover.admin.feature.appointments.domain.usecase.ValidateBookingDraftUseCase
import com.uniquemakeover.admin.feature.clients.domain.usecase.BuildRebookDraftUseCase
import com.uniquemakeover.admin.feature.services.domain.usecase.GetServicesUseCase
import com.uniquemakeover.admin.feature.settings.domain.usecase.GetOperationsConfigUseCase
import com.uniquemakeover.admin.testutil.FakeTimeProvider
import com.uniquemakeover.admin.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val service1 = Service("s1", "Gold Facial", 800)
    private val service2 = Service("s2", "Keratin Spa", 1500)

    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeServiceRepository: FakeServiceRepository
    private lateinit var fakeSettingsRepository: FakeSettingsRepository
    private lateinit var fakeAppointmentRepository: FakeAppointmentRepository
    private lateinit var fakeSlotRepository: FakeSlotRepository
    private lateinit var phoneSanitizer: PhoneSanitizer
    private lateinit var validateUseCase: ValidateBookingDraftUseCase
    private lateinit var bookUseCase: BookAppointmentUseCase
    private lateinit var editUseCase: EditAppointmentUseCase
    private lateinit var generateSlotsUseCase: GenerateAvailableSlotsUseCase
    private lateinit var viewModel: BookingViewModel

    @Before
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider("10:00", "2026-11-20")
        fakeServiceRepository = FakeServiceRepository(listOf(service1, service2))
        fakeSettingsRepository = FakeSettingsRepository()
        fakeAppointmentRepository = FakeAppointmentRepository()
        val fakePaymentRepository = FakePaymentRepository()
        fakeSlotRepository = FakeSlotRepository()
        phoneSanitizer = PhoneSanitizer()
        validateUseCase = ValidateBookingDraftUseCase(phoneSanitizer)
        bookUseCase = BookAppointmentUseCase(
            validateUseCase,
            fakeAppointmentRepository,
            fakePaymentRepository,
            fakeSlotRepository,
            phoneSanitizer,
            fakeTimeProvider
        )
        editUseCase = EditAppointmentUseCase(
            fakeAppointmentRepository,
            fakeSlotRepository,
            validateUseCase,
            phoneSanitizer
        )
        generateSlotsUseCase = GenerateAvailableSlotsUseCase(
            fakeSettingsRepository,
            fakeSlotRepository,
            fakeTimeProvider
        )
        val rebookUseCase = BuildRebookDraftUseCase(
            fakeAppointmentRepository,
            fakeServiceRepository
        )
        val bookingOperations = BookingOperations(
            bookAppointment = bookUseCase,
            editAppointment = editUseCase,
            buildRebookDraft = rebookUseCase
        )

        viewModel = BookingViewModel(
            getServicesUseCase = GetServicesUseCase(fakeServiceRepository),
            getOperationsConfigUseCase = GetOperationsConfigUseCase(fakeSettingsRepository),
            generateAvailableSlotsUseCase = generateSlotsUseCase,
            bookingOperations = bookingOperations,
            timeProvider = fakeTimeProvider
        )
    }

    @Test
    fun financialRecalculation_multiServiceSelection_computesLiveTotals() = runTest {
        advanceUntilIdle()

        viewModel.toggleService(service1) // + 800
        viewModel.toggleService(service2) // + 1500

        var draft = viewModel.uiState.value.draft
        assertEquals(2300, draft.subtotal)
        assertEquals(2300, draft.finalRate)
        assertEquals(2300, draft.balanceDue)

        viewModel.updateDiscount(300)
        viewModel.updateAdvance(500)

        draft = viewModel.uiState.value.draft
        assertEquals(2300, draft.subtotal)
        assertEquals(2000, draft.finalRate)
        assertEquals(1500, draft.balanceDue)
        assertNull(viewModel.uiState.value.financialError)
    }

    @Test
    fun advanceExceedsFinalRate_flagsFinancialError() = runTest {
        advanceUntilIdle()

        viewModel.toggleService(service1) // 800
        viewModel.updateAdvance(1000)

        assertNotNull(viewModel.uiState.value.financialError)
    }

    @Test
    fun saveAppointment_validDraft_invokesOnSuccessAndCommitsToRepository() = runTest {
        advanceUntilIdle()

        viewModel.updateName("Pooja Patil")
        viewModel.updatePhone("9822001122")
        viewModel.toggleService(service1)
        viewModel.selectSlot(Slot("10:00", "10:00 AM", isAvailable = true))
        viewModel.updateDiscount(200)
        viewModel.updateAdvance(200)

        var successCallbackInvoked = false
        viewModel.saveAppointment(onSuccess = { successCallbackInvoked = true })
        advanceUntilIdle()

        assertTrue(successCallbackInvoked)
        assertTrue(viewModel.uiState.value.bookingSuccess)
        assertEquals(1, fakeAppointmentRepository.insertedAppointments.size)
        assertEquals("Pooja Patil", fakeAppointmentRepository.insertedAppointments[0].name)
        assertEquals("9822001122", fakeAppointmentRepository.insertedAppointments[0].phone)
        assertEquals(600, fakeAppointmentRepository.insertedAppointments[0].rate)
        assertEquals(200, fakeAppointmentRepository.insertedAppointments[0].paid)
    }

    @Test
    fun loadAppointmentForEdit_populatesDraftAndSetsEditMode() = runTest {
        val existing = Appointment(
            id = "apt_101",
            name = "Pooja Patil",
            phone = "9822001122",
            serviceSnapshot = "Gold Facial",
            serviceIds = listOf("s1"),
            rate = 700,
            paid = 300,
            staff = "Sunita",
            date = "2026-11-20",
            time = "11:00",
            note = "Allergic to strong bleach",
            status = "booked",
            bookedAt = 1700000000000L
        )
        fakeAppointmentRepository.insertedAppointments.add(existing)

        viewModel.loadAppointmentForEdit("apt_101")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEditMode)
        assertEquals("apt_101", state.editingAppointmentId)
        assertFalse(state.isDateTimeLocked)
        assertEquals("Pooja Patil", state.draft.name)
        assertEquals("9822001122", state.draft.phone)
        assertEquals(1, state.draft.selectedServices.size)
        assertEquals("s1", state.draft.selectedServices[0].id)
        assertEquals(100, state.draft.discount) // 800 - 700 = 100
        assertEquals(300, state.draft.advancePaid)
        assertEquals("Allergic to strong bleach", state.draft.internalNote)
    }

    @Test
    fun loadAppointmentForEdit_locksDateTimeForCompletedAppointments() = runTest {
        val existing = Appointment(
            id = "apt_102",
            name = "Rani Deshmukh",
            phone = "9822003344",
            serviceSnapshot = "Keratin Spa",
            serviceIds = listOf("s2"),
            rate = 1500,
            paid = 1500,
            staff = "Sunita",
            date = "2026-11-20",
            time = "14:00",
            note = "",
            status = "done",
            bookedAt = 1700000000000L
        )
        fakeAppointmentRepository.insertedAppointments.add(existing)

        viewModel.loadAppointmentForEdit("apt_102")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEditMode)
        assertTrue(state.isDateTimeLocked)
    }

    @Test
    fun saveAppointment_inEditMode_updatesExistingAppointment() = runTest {
        val existing = Appointment(
            id = "apt_101",
            name = "Pooja Patil",
            phone = "9822001122",
            serviceSnapshot = "Gold Facial",
            serviceIds = listOf("s1"),
            rate = 800,
            paid = 0,
            staff = "Sunita",
            date = "2026-11-20",
            time = "11:00",
            note = "",
            status = "booked",
            bookedAt = 1700000000000L
        )
        fakeAppointmentRepository.insertedAppointments.add(existing)

        viewModel.loadAppointmentForEdit("apt_101")
        advanceUntilIdle()

        // Modify note and add service2
        viewModel.updateNote("Updated preference")
        viewModel.toggleService(service2)

        var saved = false
        viewModel.saveAppointment { saved = true }
        advanceUntilIdle()

        assertTrue(saved)
        assertTrue(viewModel.uiState.value.bookingSuccess)
        val updated = fakeAppointmentRepository.insertedAppointments.find { it.id == "apt_101" }
        assertNotNull(updated)
        assertEquals("Updated preference", updated?.note)
        assertEquals("Gold Facial + Keratin Spa", updated?.serviceSnapshot)
        assertEquals(2300, updated?.rate)
    }

    private class FakeServiceRepository(private val services: List<Service>) : ServiceRepository {
        override fun getServices(): Flow<List<Service>> = flowOf(services)
        override suspend fun getServiceById(id: String): Service? = services.find { it.id == id }
        override suspend fun saveService(service: Service): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun deleteService(id: String): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun countDuplicateNames(name: String, excludeId: String): Int = 0
    }

    @Test
    fun loadRebookDraft_prePopulatesClientDetailsAndServices() = runTest {
        val historicalApt = Appointment(
            id = "apt_hist_1",
            name = "Kavita",
            phone = "9822001122",
            serviceSnapshot = "Keratin Spa",
            serviceIds = listOf("s1"),
            rate = 800,
            paid = 800,
            staff = "Sunita",
            date = "2026-10-10",
            time = "10:00",
            note = "",
            status = "done",
            reminded = null,
            bookedAt = 1000L
        )
        fakeAppointmentRepository.insertedAppointments.add(historicalApt)

        viewModel.loadRebookDraft("9822001122")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Kavita", state.draft.name)
        assertEquals("9822001122", state.draft.phone)
        assertEquals("2026-11-20", state.draft.date)
        assertEquals("", state.draft.time)
        assertEquals(1, state.draft.selectedServices.size)
        assertEquals("s1", state.draft.selectedServices[0].id)
        assertEquals(800, state.draft.subtotal)
    }

    private class FakeSettingsRepository : SettingsRepository {
        val config = OperationsConfig("09:00", "19:00", 30, listOf("Sunita", "Pooja"))
        override fun getSettings(): Flow<Settings?> = emptyFlow()
        override suspend fun getSettingsSync(): Settings? = null
        override suspend fun getOperationsConfig(): OperationsConfig = config
        override suspend fun updateOperationsConfig(newConfig: OperationsConfig): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun updateSettings(settings: Settings): DataResult<Unit> = DataResult.Success(Unit)
    }

    private class FakeAppointmentRepository : AppointmentRepository {
        val insertedAppointments = mutableListOf<Appointment>()
        override fun getAllAppointments(): Flow<List<Appointment>> = flowOf(insertedAppointments)
        override fun getAppointmentsByPhone(phone: String): Flow<List<Appointment>> =
            flowOf(insertedAppointments.filter { it.phone.endsWith(phone.takeLast(10)) })
        override fun getTodaySchedule(today: String): Flow<List<Appointment>> = emptyFlow()
        override fun getAppointmentsByDate(date: String): Flow<List<Appointment>> = emptyFlow()
        override suspend fun getAppointmentsByDateSync(date: String): List<Appointment> = emptyList()
        override fun getFilteredAppointments(
            status: String?,
            startDate: String?,
            endDate: String?,
            query: String
        ): Flow<List<Appointment>> = flowOf(insertedAppointments)
        override suspend fun insertAppointment(appointment: Appointment): DataResult<Unit> {
            insertedAppointments.add(appointment)
            return DataResult.Success(Unit)
        }
        override suspend fun updateAppointment(appointment: Appointment): DataResult<Unit> {
            val index = insertedAppointments.indexOfFirst { it.id == appointment.id }
            if (index >= 0) {
                insertedAppointments[index] = appointment
            }
            return DataResult.Success(Unit)
        }
        override suspend fun updateStatus(id: String, status: AppointmentStatus): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun getAppointmentById(id: String): Appointment? = insertedAppointments.find { it.id == id }
        override suspend fun deleteAppointment(appointment: Appointment): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun deleteById(id: String): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun countUpcomingAppointmentsForService(serviceId: String, today: String): Int = 0
        override suspend fun count(): Int = insertedAppointments.size
    }

    private class FakeSlotRepository : SlotRepository {
        override suspend fun isClashFree(date: String, time: String, excludeId: String?): Boolean = true
        override suspend fun getAvailableSlots(date: String): List<Slot> = emptyList()
    }

    private class FakePaymentRepository : PaymentRepository {
        override fun getPaymentsForAppointment(appointmentId: String): Flow<List<Payment>> = emptyFlow()
        override suspend fun getPaymentsForAppointmentSync(appointmentId: String): List<Payment> = emptyList()
        override suspend fun recordPayment(payment: Payment): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun deletePaymentsByAppointmentId(appointmentId: String): DataResult<Unit> =
            DataResult.Success(Unit)
    }
}
