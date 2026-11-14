package com.uniquemakeover.admin.feature.appointments.presentation.booking

import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.core.domain.model.Slot
import com.uniquemakeover.admin.core.domain.repository.ServiceRepository
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.PhoneSanitizer
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.appointments.domain.repository.SlotRepository
import com.uniquemakeover.admin.feature.appointments.domain.usecase.BookAppointmentUseCase
import com.uniquemakeover.admin.feature.appointments.domain.usecase.GenerateAvailableSlotsUseCase
import com.uniquemakeover.admin.feature.appointments.domain.usecase.ValidateBookingDraftUseCase
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
    private lateinit var generateSlotsUseCase: GenerateAvailableSlotsUseCase
    private lateinit var viewModel: BookingViewModel

    @Before
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider("10:00", "2026-11-20")
        fakeServiceRepository = FakeServiceRepository(listOf(service1, service2))
        fakeSettingsRepository = FakeSettingsRepository()
        fakeAppointmentRepository = FakeAppointmentRepository()
        fakeSlotRepository = FakeSlotRepository()
        phoneSanitizer = PhoneSanitizer()
        validateUseCase = ValidateBookingDraftUseCase(phoneSanitizer)
        bookUseCase = BookAppointmentUseCase(
            validateUseCase,
            fakeAppointmentRepository,
            fakeSlotRepository,
            phoneSanitizer,
            fakeTimeProvider
        )
        generateSlotsUseCase = GenerateAvailableSlotsUseCase(
            fakeSettingsRepository,
            fakeSlotRepository,
            fakeTimeProvider
        )

        viewModel = BookingViewModel(
            getServicesUseCase = GetServicesUseCase(fakeServiceRepository),
            getOperationsConfigUseCase = GetOperationsConfigUseCase(fakeSettingsRepository),
            generateAvailableSlotsUseCase = generateSlotsUseCase,
            bookAppointmentUseCase = bookUseCase,
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

    private class FakeServiceRepository(private val services: List<Service>) : ServiceRepository {
        override fun getServices(): Flow<List<Service>> = flowOf(services)
        override suspend fun getServiceById(id: String): Service? = services.find { it.id == id }
        override suspend fun saveService(service: Service) = Unit
        override suspend fun deleteService(id: String) = Unit
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
        override fun getAppointmentsByDate(date: String): Flow<List<Appointment>> = emptyFlow()
        override suspend fun getAppointmentsByDateSync(date: String): List<Appointment> = emptyList()
        override suspend fun insertAppointment(appointment: Appointment): DataResult<Unit> {
            insertedAppointments.add(appointment)
            return DataResult.Success(Unit)
        }
        override suspend fun updateAppointment(appointment: Appointment): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun deleteAppointment(appointment: Appointment): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun count(): Int = insertedAppointments.size
    }

    private class FakeSlotRepository : SlotRepository {
        override suspend fun isClashFree(date: String, time: String, excludeId: String?): Boolean = true
        override suspend fun getAvailableSlots(date: String): List<Slot> = emptyList()
    }
}
