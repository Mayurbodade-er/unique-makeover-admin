package com.uniquemakeover.admin.feature.dashboard.presentation

import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.core.domain.model.Slot
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.appointments.domain.repository.SlotRepository
import com.uniquemakeover.admin.feature.appointments.domain.usecase.CancelAppointmentUseCase
import com.uniquemakeover.admin.feature.appointments.domain.usecase.MarkAppointmentDoneUseCase
import com.uniquemakeover.admin.feature.appointments.domain.usecase.UndoAppointmentStatusUseCase
import com.uniquemakeover.admin.feature.dashboard.domain.usecase.GetDashboardKpisUseCase
import com.uniquemakeover.admin.feature.dashboard.domain.usecase.GetTodayScheduleUseCase
import com.uniquemakeover.admin.testutil.FakeTimeProvider
import com.uniquemakeover.admin.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeAppointmentRepository: FakeAppointmentRepository
    private lateinit var fakeSlotRepository: FakeSlotRepository
    private lateinit var getDashboardKpisUseCase: GetDashboardKpisUseCase
    private lateinit var getTodayScheduleUseCase: GetTodayScheduleUseCase
    private lateinit var markDoneUseCase: MarkAppointmentDoneUseCase
    private lateinit var cancelUseCase: CancelAppointmentUseCase
    private lateinit var undoUseCase: UndoAppointmentStatusUseCase
    private lateinit var viewModel: DashboardViewModel

    private val sampleAppointment = Appointment(
        id = "apt_1",
        name = "Sunita Sharma",
        phone = "9876543210",
        serviceSnapshot = "Gold Facial",
        serviceIds = listOf("s1"),
        rate = 1000,
        paid = 400,
        staff = "Sunita",
        date = "2026-11-20",
        time = "10:00",
        note = "",
        status = "booked",
        bookedAt = 1000L
    )

    @Before
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeTimeProvider.currentDate = "2026-11-20"
        fakeTimeProvider.currentTime24 = "09:00"
        fakeAppointmentRepository = FakeAppointmentRepository(mutableListOf(sampleAppointment))
        fakeSlotRepository = FakeSlotRepository()
        getDashboardKpisUseCase = GetDashboardKpisUseCase(
            appointmentRepository = fakeAppointmentRepository,
            timeProvider = fakeTimeProvider
        )
        getTodayScheduleUseCase = GetTodayScheduleUseCase(
            appointmentRepository = fakeAppointmentRepository,
            timeProvider = fakeTimeProvider
        )
        markDoneUseCase = MarkAppointmentDoneUseCase(fakeAppointmentRepository)
        cancelUseCase = CancelAppointmentUseCase(fakeAppointmentRepository)
        undoUseCase = UndoAppointmentStatusUseCase(fakeAppointmentRepository, fakeSlotRepository)

        viewModel = DashboardViewModel(
            getDashboardKpisUseCase = getDashboardKpisUseCase,
            getTodayScheduleUseCase = getTodayScheduleUseCase,
            markAppointmentDoneUseCase = markDoneUseCase,
            cancelAppointmentUseCase = cancelUseCase,
            undoAppointmentStatusUseCase = undoUseCase,
            timeProvider = fakeTimeProvider
        )
    }

    @Test
    fun init_loadsKpisAndScheduleForTodayDate() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals("2026-11-20", state.todayDate)
        assertFalse(state.isLoading)
        assertEquals(1, state.kpis.bookedCount)
        assertEquals(1000, state.kpis.scheduledVolume)
        assertEquals(400, state.kpis.collectedRevenue)
        assertEquals(600, state.kpis.balanceOutstanding)
        assertEquals(1, state.schedule.size)
        assertEquals("apt_1", state.schedule[0].appointment.id)
        assertFalse(state.schedule[0].isPastTime)
    }

    @Test
    fun refresh_reloadsKpisAndSchedule() = runTest {
        advanceUntilIdle()
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("2026-11-20", state.todayDate)
        assertEquals(1, state.kpis.bookedCount)
        assertEquals(1, state.schedule.size)
    }

    @Test
    fun markDone_emitsShowUndoSnackbar() = runTest {
        val effects = mutableListOf<DashboardUiEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.markDone(sampleAppointment)
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertTrue(effects[0] is DashboardUiEffect.ShowUndoSnackbar)
        val undoEffect = effects[0] as DashboardUiEffect.ShowUndoSnackbar
        assertEquals("apt_1", undoEffect.appointmentId)
        assertEquals(AppointmentStatus.BOOKED, undoEffect.previousStatus)
    }

    @Test
    fun cancel_emitsShowUndoSnackbar() = runTest {
        val effects = mutableListOf<DashboardUiEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.cancel(sampleAppointment)
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertTrue(effects[0] is DashboardUiEffect.ShowUndoSnackbar)
        val undoEffect = effects[0] as DashboardUiEffect.ShowUndoSnackbar
        assertEquals("apt_1", undoEffect.appointmentId)
    }

    @Test
    fun undoStatus_revertsStatusAndEmitsMessage() = runTest {
        val effects = mutableListOf<DashboardUiEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.undoStatus("apt_1", AppointmentStatus.BOOKED)
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertTrue(effects[0] is DashboardUiEffect.ShowMessage)
    }

    private class FakeAppointmentRepository(
        private val list: MutableList<Appointment>
    ) : AppointmentRepository {
        override fun getAllAppointments(): Flow<List<Appointment>> = flowOf(list)
        override fun getAppointmentsByDate(date: String): Flow<List<Appointment>> = flowOf(list)
        override suspend fun getAppointmentsByDateSync(date: String): List<Appointment> = list
        override fun getTodaySchedule(today: String): Flow<List<Appointment>> = flowOf(list)
        override fun getFilteredAppointments(
            status: String?,
            startDate: String?,
            endDate: String?,
            query: String
        ): Flow<List<Appointment>> = flowOf(list)

        override suspend fun insertAppointment(appointment: Appointment): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun updateAppointment(appointment: Appointment): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun updateStatus(id: String, status: AppointmentStatus): DataResult<Unit> {
            val idx = list.indexOfFirst { it.id == id }
            if (idx >= 0) list[idx] = list[idx].copy(status = status.value)
            return DataResult.Success(Unit)
        }
        override suspend fun getAppointmentById(id: String): Appointment? = list.find { it.id == id }
        override suspend fun deleteAppointment(appointment: Appointment): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun count(): Int = list.size
    }

    private class FakeSlotRepository : SlotRepository {
        override suspend fun isClashFree(date: String, time: String, excludeId: String?): Boolean = true
        override suspend fun getAvailableSlots(date: String): List<Slot> = emptyList()
    }
}
