package com.uniquemakeover.admin.feature.reminders.presentation

import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.reminders.domain.model.ReminderQueueItem
import com.uniquemakeover.admin.feature.reminders.domain.repository.RemindersRepository
import com.uniquemakeover.admin.feature.reminders.domain.usecase.GetTomorrowQueueUseCase
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
class RemindersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeRemindersRepository
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var getTomorrowQueueUseCase: GetTomorrowQueueUseCase
    private lateinit var viewModel: RemindersViewModel

    private val sampleAppointment1 = Appointment(
        id = "apt-1",
        name = "Priya",
        phone = "9822112233",
        serviceSnapshot = "Facial",
        serviceIds = listOf("srv-1"),
        rate = 800,
        paid = 500,
        staff = "Sunita",
        date = "2026-11-15",
        time = "10:30 AM",
        note = "",
        status = "booked",
        reminded = null,
        bookedAt = 1000L
    )

    private val sampleAppointment2 = Appointment(
        id = "apt-2",
        name = "Kavita",
        phone = "9822445566",
        serviceSnapshot = "Hair Cut",
        serviceIds = listOf("srv-2"),
        rate = 400,
        paid = 400,
        staff = "Pooja",
        date = "2026-11-15",
        time = "11:30 AM",
        note = "",
        status = "booked",
        reminded = "2026-11-15",
        bookedAt = 1010L
    )

    @Before
    fun setUp() {
        fakeRepository = FakeRemindersRepository()
        fakeTimeProvider = FakeTimeProvider(currentDate = "2026-11-14")
        getTomorrowQueueUseCase = GetTomorrowQueueUseCase(fakeRepository)
    }

    @Test
    fun `loadTomorrowQueue updates state with correct queue and counters`() = runTest {
        fakeRepository.queue = listOf(
            ReminderQueueItem(sampleAppointment1, isReminded = false),
            ReminderQueueItem(sampleAppointment2, isReminded = true)
        )

        viewModel = RemindersViewModel(getTomorrowQueueUseCase, fakeTimeProvider)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.totalCount)
        assertEquals(1, state.remindedCount)
        assertEquals(2, state.queue.size)
        assertTrue(state.tomorrowDateDisplay.contains("Tomorrow"))
        assertTrue(state.tomorrowDateDisplay.contains("15 Nov 2026"))
    }

    @Test
    fun `loadTomorrowQueue with empty list updates state with zero counts`() = runTest {
        fakeRepository.queue = emptyList()

        viewModel = RemindersViewModel(getTomorrowQueueUseCase, fakeTimeProvider)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(0, state.totalCount)
        assertEquals(0, state.remindedCount)
        assertTrue(state.queue.isEmpty())
    }

    private class FakeRemindersRepository : RemindersRepository {
        var queue: List<ReminderQueueItem> = emptyList()

        override fun getTomorrowQueue(): Flow<List<ReminderQueueItem>> = flowOf(queue)

        override suspend fun markReminded(appointmentId: String, tomorrowDate: String): DataResult<Unit> =
            DataResult.Success(Unit)

        override suspend fun undoMarkReminded(appointmentId: String): DataResult<Unit> =
            DataResult.Success(Unit)
    }
}
