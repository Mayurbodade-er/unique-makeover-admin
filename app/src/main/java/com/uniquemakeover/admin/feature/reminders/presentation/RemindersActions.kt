package com.uniquemakeover.admin.feature.reminders.presentation

import com.uniquemakeover.admin.feature.reminders.domain.model.ReminderQueueItem

data class RemindersActions(
    val onRemindClick: (ReminderQueueItem) -> Unit = {},
    val onMarkNotSent: (String) -> Unit = {},
    val onUndoClick: () -> Unit = {},
    val onDismissSnackbar: () -> Unit = {},
    val onDismissClipboardDialog: () -> Unit = {}
)
