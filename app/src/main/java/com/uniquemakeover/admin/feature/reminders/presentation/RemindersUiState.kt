package com.uniquemakeover.admin.feature.reminders.presentation

import com.uniquemakeover.admin.feature.reminders.domain.model.ReminderQueueItem

data class RemindersUiState(
    val isLoading: Boolean = false,
    val tomorrowDateDisplay: String = "",
    val queue: List<ReminderQueueItem> = emptyList(),
    val totalCount: Int = 0,
    val remindedCount: Int = 0,
    val errorMessage: String? = null
)
