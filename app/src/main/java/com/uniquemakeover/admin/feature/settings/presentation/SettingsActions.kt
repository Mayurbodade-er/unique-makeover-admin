package com.uniquemakeover.admin.feature.settings.presentation

data class BrandingDraft(
    val shopName: String,
    val tagline: String,
    val logoUriString: String? = null,
    val wallpaperUriString: String? = null,
    val removeLogo: Boolean = false,
    val removeWallpaper: Boolean = false
)

data class SettingsActions(
    val onSaveBranding: (BrandingDraft) -> Unit = {},
    val onSaveOperations: (String, String, Int, String) -> Unit = { _, _, _, _ -> },
    val onSaveCommunication: (String, String) -> Unit = { _, _ -> },
    val onSaveReminderTemplate: (String) -> Unit = {},
    val onChangePin: (String, String, String) -> Unit = { _, _, _ -> },
    val onExportBackup: (String) -> Unit = {},
    val onClearMessages: () -> Unit = {}
)
