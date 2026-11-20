package com.uniquemakeover.admin.feature.reminders.domain.repository

import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.reminders.domain.model.ReminderQueueItem
import kotlinx.coroutines.flow.Flow

interface RemindersRepository {
    fun getTomorrowQueue(): Flow<List<ReminderQueueItem>>
    suspend fun markReminded(appointmentId: String, tomorrowDate: String): DataResult<Unit>
    suspend fun undoMarkReminded(appointmentId: String): DataResult<Unit>
}
