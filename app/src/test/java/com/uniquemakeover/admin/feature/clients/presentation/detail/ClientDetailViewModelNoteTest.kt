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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClientDetailViewModelNoteTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeAppointmentRepo = FakeAppointmentRepo()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onNoteChange_debouncesSave_andTransitionsStatus() = runTest(testDispatcher) {
        val savedNotes = mutableListOf<String>()

        val fakeNoteRepo = object : ClientNoteRepository {
            override fun getNote(phone: String): Flow<ClientNote?> = flowOf(null)

            override suspend fun saveNote(phone: String, noteText: String): DataResult<Unit> {
                savedNotes.add(noteText)
                return DataResult.Success(Unit)
            }
        }

        val savedStateHandle = SavedStateHandle(mapOf("phone" to "9822001122"))
        val historyUseCase = GetClientHistoryUseCase(fakeAppointmentRepo)
        val getNoteUseCase = GetClientNoteUseCase(fakeNoteRepo)
        val saveNoteUseCase = SaveClientNoteUseCase(fakeNoteRepo)

        val viewModel = ClientDetailViewModel(
            savedStateHandle = savedStateHandle,
            appointmentRepository = fakeAppointmentRepo,
            getClientHistoryUseCase = historyUseCase,
            getClientNoteUseCase = getNoteUseCase,
            saveClientNoteUseCase = saveNoteUseCase
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        // User types text
        viewModel.onNoteChange("Allergic to ammonia")
        testScheduler.runCurrent()
        assertEquals("Allergic to ammonia", viewModel.uiState.value.noteText)
        assertEquals(NoteSaveStatus.SAVING, viewModel.uiState.value.noteSaveStatus)
        assertEquals(0, savedNotes.size)

        // Advance 300ms (not yet debounced)
        advanceTimeBy(300)
        testScheduler.runCurrent()
        assertEquals(0, savedNotes.size)
        assertEquals(NoteSaveStatus.SAVING, viewModel.uiState.value.noteSaveStatus)

        // Advance remaining 250ms (total 550ms > 500ms debounce)
        advanceTimeBy(250)
        advanceUntilIdle()

        assertEquals(1, savedNotes.size)
        assertEquals("Allergic to ammonia", savedNotes[0])
        assertEquals(NoteSaveStatus.SAVED, viewModel.uiState.value.noteSaveStatus)
    }

    @Test
    fun flushPendingNote_savesImmediately_withoutWaitingForDebounce() = runTest(testDispatcher) {
        val savedNotes = mutableListOf<String>()

        val fakeNoteRepo = object : ClientNoteRepository {
            override fun getNote(phone: String): Flow<ClientNote?> = flowOf(null)

            override suspend fun saveNote(phone: String, noteText: String): DataResult<Unit> {
                savedNotes.add(noteText)
                return DataResult.Success(Unit)
            }
        }

        val savedStateHandle = SavedStateHandle(mapOf("phone" to "9822001122"))
        val historyUseCase = GetClientHistoryUseCase(fakeAppointmentRepo)
        val getNoteUseCase = GetClientNoteUseCase(fakeNoteRepo)
        val saveNoteUseCase = SaveClientNoteUseCase(fakeNoteRepo)

        val viewModel = ClientDetailViewModel(
            savedStateHandle = savedStateHandle,
            appointmentRepository = fakeAppointmentRepo,
            getClientHistoryUseCase = historyUseCase,
            getClientNoteUseCase = getNoteUseCase,
            saveClientNoteUseCase = saveNoteUseCase
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onNoteChange("Emergency note before exit")
        assertEquals(0, savedNotes.size)

        // Immediate flush
        viewModel.flushPendingNote()
        advanceUntilIdle()

        assertEquals(1, savedNotes.size)
        assertEquals("Emergency note before exit", savedNotes[0])
        assertEquals(NoteSaveStatus.SAVED, viewModel.uiState.value.noteSaveStatus)
    }

    private class FakeAppointmentRepo : AppointmentRepository {
        override fun getAllAppointments(): Flow<List<Appointment>> = flowOf(emptyList())
        override fun getAppointmentsByPhone(phone: String): Flow<List<Appointment>> = flowOf(emptyList())
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
}
