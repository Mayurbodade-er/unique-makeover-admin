package com.uniquemakeover.admin.feature.reminders.domain.usecase

import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.reminders.domain.model.ReminderQueueItem
import com.uniquemakeover.admin.feature.reminders.domain.repository.RemindersRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UndoMarkRemindedUseCaseTest {

    private lateinit var useCase: UndoMarkRemindedUseCase
    private lateinit var fakeRepository: FakeRemindersRepository

    @Before
    fun setUp() {
        fakeRepository = FakeRemindersRepository()
        useCase = UndoMarkRemindedUseCase(fakeRepository)
    }

    @Test
    fun `delegates undo mark reminded to repository`() = runBlocking {
        val result = useCase("apt-123")

        assertTrue(result is DataResult.Success)
        assertEquals("apt-123", fakeRepository.lastUndoId)
    }

    private class FakeRemindersRepository : RemindersRepository {
        var lastUndoId: String? = null

        override fun getTomorrowQueue(): Flow<List<ReminderQueueItem>> = flowOf(emptyList())

        override suspend fun markReminded(appointmentId: String, tomorrowDate: String): DataResult<Unit> =
            DataResult.Success(Unit)

        override suspend fun undoMarkReminded(appointmentId: String): DataResult<Unit> {
            lastUndoId = appointmentId
            return DataResult.Success(Unit)
        }
    }
}
