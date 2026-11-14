package com.uniquemakeover.admin.feature.dashboard.presentation

import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.dashboard.domain.usecase.GetDashboardKpisUseCase
import com.uniquemakeover.admin.testutil.FakeTimeProvider
import com.uniquemakeover.admin.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeAppointmentRepository: FakeAppointmentRepository
    private lateinit var getDashboardKpisUseCase: GetDashboardKpisUseCase
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
        fakeAppointmentRepository = FakeAppointmentRepository(listOf(sampleAppointment))
        getDashboardKpisUseCase = GetDashboardKpisUseCase(
            appointmentRepository = fakeAppointmentRepository,
            timeProvider = fakeTimeProvider
        )
        viewModel = DashboardViewModel(
            getDashboardKpisUseCase = getDashboardKpisUseCase,
            timeProvider = fakeTimeProvider
        )
    }

    @Test
    fun init_loadsKpisForTodayDate() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals("2026-11-20", state.todayDate)
        assertFalse(state.isLoading)
        assertEquals(1, state.kpis.bookedCount)
        assertEquals(1000, state.kpis.scheduledVolume)
        assertEquals(400, state.kpis.collectedRevenue)
        assertEquals(600, state.kpis.balanceOutstanding)
    }

    @Test
    fun refresh_reloadsKpis() = runTest {
        advanceUntilIdle()
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("2026-11-20", state.todayDate)
        assertEquals(1, state.kpis.bookedCount)
    }

    private class FakeAppointmentRepository(
        private val initialList: List<Appointment>
    ) : AppointmentRepository {
        override fun getAllAppointments(): Flow<List<Appointment>> = flowOf(initialList)
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
