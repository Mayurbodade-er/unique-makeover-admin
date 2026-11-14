package com.uniquemakeover.admin.feature.appointments.presentation

import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.core.domain.model.Slot
import com.uniquemakeover.admin.core.domain.model.UnavailableReason
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.PhoneSanitizer
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.model.DateRange
import com.uniquemakeover.admin.feature.appointments.domain.model.FilterTab
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.appointments.domain.repository.SlotRepository
import com.uniquemakeover.admin.feature.appointments.domain.usecase.GenerateAvailableSlotsUseCase
import com.uniquemakeover.admin.feature.appointments.domain.usecase.GetAppointmentsUseCase
import com.uniquemakeover.admin.testutil.FakeTimeProvider
import com.uniquemakeover.admin.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeSettingsRepository: FakeSettingsRepository
    private lateinit var fakeSlotRepository: FakeSlotRepository
    private lateinit var fakeAppointmentRepository: FakeAppointmentRepository
    private lateinit var generateSlotsUseCase: GenerateAvailableSlotsUseCase
    private lateinit var getAppointmentsUseCase: GetAppointmentsUseCase
    private lateinit var viewModel: AppointmentsViewModel

    private val sampleAppointment = Appointment(
        id = "apt_1",
        name = "Sunita Sharma",
        phone = "9876543210",
        serviceSnapshot = "Gold Facial",
        serviceIds = listOf("s1"),
        rate = 1000,
        paid = 500,
        staff = "Sunita",
        date = "2026-11-20",
        time = "10:00",
        note = "",
        status = "booked",
        reminded = null,
        bookedAt = 1000L
    )

    @Before
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider("10:00", "2026-11-20")
        fakeSettingsRepository = FakeSettingsRepository()
        fakeSlotRepository = FakeSlotRepository()
        fakeAppointmentRepository = FakeAppointmentRepository(listOf(sampleAppointment))
        generateSlotsUseCase = GenerateAvailableSlotsUseCase(
            settingsRepository = fakeSettingsRepository,
            slotRepository = fakeSlotRepository,
            timeProvider = fakeTimeProvider
        )
        getAppointmentsUseCase = GetAppointmentsUseCase(
            appointmentRepository = fakeAppointmentRepository,
            timeProvider = fakeTimeProvider,
            phoneSanitizer = PhoneSanitizer()
        )
        viewModel = AppointmentsViewModel(
            generateAvailableSlotsUseCase = generateSlotsUseCase,
            getAppointmentsUseCase = getAppointmentsUseCase,
            appointmentRepository = fakeAppointmentRepository,
            timeProvider = fakeTimeProvider
        )
    }

    @Test
    fun init_loadsSlotsAndAppointmentsAndTabCounts() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals("2026-11-20", state.selectedDate)
        assertEquals(6, state.slots.size)
        assertEquals(1, state.appointments.size)
        assertEquals("Sunita Sharma", state.appointments[0].name)
        assertEquals(1, state.tabCounts.all)
        assertEquals(1, state.tabCounts.upcoming)
    }

    @Test
    fun updateTab_updatesFilter() = runTest {
        advanceUntilIdle()
        viewModel.updateTab(FilterTab.COMPLETED)
        advanceUntilIdle()

        assertEquals(FilterTab.COMPLETED, viewModel.uiState.value.filter.tab)
    }

    @Test
    fun updateSearchQuery_updatesFilter() = runTest {
        advanceUntilIdle()
        viewModel.updateSearchQuery("Sharma")
        advanceUntilIdle()

        assertEquals("Sharma", viewModel.uiState.value.filter.searchQuery)
    }

    @Test
    fun updateDateRange_updatesFilter() = runTest {
        advanceUntilIdle()
        val range = DateRange("2026-11-01", "2026-11-15")
        viewModel.updateDateRange(range)
        advanceUntilIdle()

        assertEquals(range, viewModel.uiState.value.filter.dateRange)
    }

    @Test
    fun clearFilters_resetsFilter() = runTest {
        advanceUntilIdle()
        viewModel.updateTab(FilterTab.COMPLETED)
        viewModel.updateSearchQuery("test")
        viewModel.clearFilters()
        advanceUntilIdle()

        assertEquals(FilterTab.ALL, viewModel.uiState.value.filter.tab)
        assertEquals("", viewModel.uiState.value.filter.searchQuery)
        assertNull(viewModel.uiState.value.filter.dateRange)
    }

    @Test
    fun selectSlot_availableSlot_updatesSelectedSlot() = runTest {
        advanceUntilIdle()
        val openSlot = viewModel.uiState.value.slots.first { it.isAvailable }

        viewModel.selectSlot(openSlot)
        assertEquals(openSlot.time24, viewModel.uiState.value.selectedSlot?.time24)
    }

    @Test
    fun selectSlot_unavailableSlot_doesNotSelect() = runTest {
        advanceUntilIdle()
        val unavailableSlot = Slot("12:00", "12:00 PM", isAvailable = false, reason = UnavailableReason.ALREADY_BOOKED)

        viewModel.selectSlot(unavailableSlot)
        assertNull(viewModel.uiState.value.selectedSlot)
    }

    @Test
    fun selectDate_updatesDateAndReloadsSlots() = runTest {
        advanceUntilIdle()
        viewModel.selectDate("2026-11-25")
        advanceUntilIdle()

        assertEquals("2026-11-25", viewModel.uiState.value.selectedDate)
        assertEquals(6, viewModel.uiState.value.slots.size)
    }

    private class FakeSettingsRepository : SettingsRepository {
        var config = OperationsConfig(
            openTime = "10:00",
            closeTime = "13:00",
            slotDurationMinutes = 30,
            staffList = listOf("Sunita")
        )

        override fun getSettings(): Flow<Settings?> = flowOf(null)
        override suspend fun getSettingsSync(): Settings? = null
        override suspend fun getOperationsConfig(): OperationsConfig = config
        override suspend fun updateOperationsConfig(newConfig: OperationsConfig): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun updateSettings(settings: Settings): DataResult<Unit> = DataResult.Success(Unit)
    }

    private class FakeSlotRepository : SlotRepository {
        override suspend fun isClashFree(date: String, time: String, excludeId: String?): Boolean = true
        override suspend fun getAvailableSlots(date: String): List<Slot> = emptyList()
    }

    private class FakeAppointmentRepository(
        private val initialList: List<Appointment>
    ) : AppointmentRepository {
        override fun getAllAppointments(): Flow<List<Appointment>> = flowOf(initialList)
        override fun getTodaySchedule(today: String): Flow<List<Appointment>> =
            flowOf(initialList.filter { it.date == today })
        override fun getAppointmentsByDate(date: String): Flow<List<Appointment>> = flowOf(initialList)
        override suspend fun getAppointmentsByDateSync(date: String): List<Appointment> = initialList
        override fun getFilteredAppointments(
            status: String?,
            startDate: String?,
            endDate: String?,
            query: String
        ): Flow<List<Appointment>> = flowOf(initialList)
        override suspend fun insertAppointment(appointment: Appointment): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun updateAppointment(appointment: Appointment): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun deleteAppointment(appointment: Appointment): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun count(): Int = initialList.size
    }
}
