package com.uniquemakeover.admin.feature.reminders.domain.usecase

import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.reminders.domain.model.ReminderQueueItem
import com.uniquemakeover.admin.feature.reminders.domain.repository.RemindersRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetTomorrowQueueUseCaseTest {

    private lateinit var useCase: GetTomorrowQueueUseCase
    private lateinit var fakeRepository: FakeRemindersRepository

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
        useCase = GetTomorrowQueueUseCase(fakeRepository)
    }

    @Test
    fun `returns tomorrow queue from repository with correct reminded flags`() = runBlocking {
        fakeRepository.queue = listOf(
            ReminderQueueItem(sampleAppointment1, isReminded = false),
            ReminderQueueItem(sampleAppointment2, isReminded = true)
        )

        val queue = useCase().first()

        assertEquals(2, queue.size)
        assertEquals("Priya", queue[0].appointment.name)
        assertFalse(queue[0].isReminded)
        assertEquals("Kavita", queue[1].appointment.name)
        assertTrue(queue[1].isReminded)
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
