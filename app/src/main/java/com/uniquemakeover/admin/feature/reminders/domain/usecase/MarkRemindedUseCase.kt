package com.uniquemakeover.admin.feature.reminders.domain.usecase

import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.reminders.domain.repository.RemindersRepository
import javax.inject.Inject

class MarkRemindedUseCase @Inject constructor(
    private val remindersRepository: RemindersRepository
) {
    suspend operator fun invoke(appointmentId: String, tomorrowDate: String): DataResult<Unit> =
        remindersRepository.markReminded(appointmentId, tomorrowDate)
}
