package com.uniquemakeover.admin.feature.clients.presentation.detail

import androidx.lifecycle.SavedStateHandle
import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.core.domain.model.ClientNote
import com.uniquemakeover.admin.core.domain.repository.ClientNoteRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.clients.domain.usecase.GetClientHistoryUseCase
import com.uniquemakeover.admin.feature.clients.domain.usecase.GetClientNoteUseCase
import com.uniquemakeover.admin.feature.clients.domain.usecase.SaveClientNoteUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClientDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakeNoteRepo = object : ClientNoteRepository {
        override fun getNote(phone: String): Flow<ClientNote?> = flowOf(null)
        override suspend fun saveNote(phone: String, noteText: String): DataResult<Unit> =
            DataResult.Success(Unit)
    }
    private val getNoteUseCase = GetClientNoteUseCase(fakeNoteRepo)
    private val saveNoteUseCase = SaveClientNoteUseCase(fakeNoteRepo)

    private val sampleAppointments = listOf(
        Appointment(
            id = "apt_1",
            name = "Neha",
            phone = "9822001122",
            serviceSnapshot = "Gold Facial",
            serviceIds = listOf("s1"),
            rate = 1500,
            paid = 1500,
            staff = "Sunita",
            date = "2026-10-10",
            time = "10:00",
            note = "",
            status = "done",
            reminded = null,
            bookedAt = 1000L
        ),
        Appointment(
            id = "apt_2",
            name = "Neha Kulkarni",
            phone = "9822001122",
            serviceSnapshot = "Keratin Spa",
            serviceIds = listOf("s2"),
            rate = 2500,
            paid = 1000,
            staff = "Sunita",
            date = "2026-11-20",
            time = "15:00",
            note = "",
            status = "booked",
            reminded = null,
            bookedAt = 5000L
        ),
        Appointment(
            id = "apt_3",
            name = "Neha Kulkarni",
            phone = "9822001122",
            serviceSnapshot = "Bridal Trial",
            serviceIds = listOf("s3"),
            rate = 3000,
            paid = 0,
            staff = "Sunita",
            date = "2026-11-22",
            time = "17:00",
            note = "",
            status = "cancel",
            reminded = null,
            bookedAt = 6000L
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_loadsMetricsAndExcludesCancelledFromSpend() = runTest(testDispatcher) {
        val fakeRepo = FakeAppointmentRepo(sampleAppointments)
        val historyUseCase = GetClientHistoryUseCase(fakeRepo)
        val savedStateHandle = SavedStateHandle(mapOf("phone" to "9822001122"))
        val viewModel = ClientDetailViewModel(
            savedStateHandle,
            fakeRepo,
            historyUseCase,
            getNoteUseCase,
            saveNoteUseCase
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Neha Kulkarni", state.clientName)
        assertEquals("9822001122", state.phone)
        // 1500 + 2500 = 4000 (cancelled 3000 excluded)
        assertEquals(4000, state.totalSpend)
        // 2 visits (cancelled excluded)
        assertEquals(2, state.totalVisits)
        // 3 items in timeline
        assertEquals(3, state.historyItems.size)
    }

    @Test
    fun onTabSelected_updatesSelectedTabIndex() = runTest(testDispatcher) {
        val fakeRepo = FakeAppointmentRepo(sampleAppointments)
        val historyUseCase = GetClientHistoryUseCase(fakeRepo)
        val savedStateHandle = SavedStateHandle(mapOf("phone" to "9822001122"))
        val viewModel = ClientDetailViewModel(
            savedStateHandle,
            fakeRepo,
            historyUseCase,
            getNoteUseCase,
            saveNoteUseCase
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.selectedTab)

        viewModel.onTabSelected(1)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun uiState_onRepositoryError_capturesErrorMessage() = runTest(testDispatcher) {
        val fakeRepo = object : AppointmentRepository {
            override fun getAllAppointments(): Flow<List<Appointment>> = flowOf(emptyList())
            override fun getAppointmentsByPhone(phone: String): Flow<List<Appointment>> = flow {
                throw IllegalStateException("Database failed")
            }
            override fun getAppointmentsByDate(date: String): Flow<List<Appointment>> = flowOf(emptyList())
            override suspend fun getAppointmentsByDateSync(date: String): List<Appointment> = emptyList()
            override suspend fun getAppointmentById(id: String): Appointment? = null
            override fun getTodaySchedule(today: String): Flow<List<Appointment>> = flowOf(emptyList())
            override fun getFilteredAppointments(
                status: String?,
                startDate: String?,
                endDate: String?,
                query: String
            ): Flow<List<Appointment>> = flowOf(emptyList())
            override suspend fun insertAppointment(
                appointment: Appointment
            ): DataResult<Unit> = DataResult.Success(Unit)
            override suspend fun updateAppointment(
                appointment: Appointment
            ): DataResult<Unit> = DataResult.Success(Unit)
            override suspend fun updateStatus(
                id: String,
                status: AppointmentStatus
            ): DataResult<Unit> = DataResult.Success(Unit)
            override suspend fun deleteAppointment(
                appointment: Appointment
            ): DataResult<Unit> = DataResult.Success(Unit)
            override suspend fun deleteById(id: String): DataResult<Unit> = DataResult.Success(Unit)
            override suspend fun countUpcomingAppointmentsForService(serviceId: String, today: String): Int = 0
            override suspend fun count(): Int = 0
        }
        val historyUseCase = GetClientHistoryUseCase(fakeRepo)
        val savedStateHandle = SavedStateHandle(mapOf("phone" to "9822001122"))
        val viewModel = ClientDetailViewModel(
            savedStateHandle,
            fakeRepo,
            historyUseCase,
            getNoteUseCase,
            saveNoteUseCase
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage?.contains("Database failed") == true)
    }

    private class FakeAppointmentRepo(private val list: List<Appointment>) : AppointmentRepository {
        override fun getAllAppointments(): Flow<List<Appointment>> = flowOf(list)
        override fun getAppointmentsByPhone(phone: String): Flow<List<Appointment>> = flowOf(list)
        override fun getAppointmentsByDate(date: String): Flow<List<Appointment>> = flowOf(list)
        override suspend fun getAppointmentsByDateSync(date: String): List<Appointment> = list
        override suspend fun getAppointmentById(id: String): Appointment? = list.find { it.id == id }
        override fun getTodaySchedule(today: String): Flow<List<Appointment>> = flowOf(list)
        override fun getFilteredAppointments(
            status: String?,
            startDate: String?,
            endDate: String?,
            query: String
        ): Flow<List<Appointment>> = flowOf(list)
        override suspend fun insertAppointment(
            appointment: Appointment
        ): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun updateAppointment(
            appointment: Appointment
        ): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun updateStatus(
            id: String,
            status: AppointmentStatus
        ): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun deleteAppointment(
            appointment: Appointment
        ): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun deleteById(id: String): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun countUpcomingAppointmentsForService(serviceId: String, today: String): Int = 0
        override suspend fun count(): Int = list.size
    }
}
