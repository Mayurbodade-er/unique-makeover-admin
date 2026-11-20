package com.uniquemakeover.admin.feature.reminders.domain.usecase

import com.uniquemakeover.admin.feature.reminders.domain.model.ReminderQueueItem
import com.uniquemakeover.admin.feature.reminders.domain.repository.RemindersRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTomorrowQueueUseCase @Inject constructor(
    private val remindersRepository: RemindersRepository
) {
    operator fun invoke(): Flow<List<ReminderQueueItem>> = remindersRepository.getTomorrowQueue()
}
