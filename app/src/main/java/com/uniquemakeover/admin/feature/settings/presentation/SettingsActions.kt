package com.uniquemakeover.admin.feature.settings.presentation

data class SettingsActions(
    val onSaveOperations: (String, String, Int, String) -> Unit = { _, _, _, _ -> },
    val onSaveCommunication: (String, String) -> Unit = { _, _ -> },
    val onSaveReminderTemplate: (String) -> Unit = {},
    val onClearMessages: () -> Unit = {}
)
