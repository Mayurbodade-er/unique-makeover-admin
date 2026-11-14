package com.uniquemakeover.admin.feature.clients.presentation.detail

data class ClientDetailActions(
    val onNavigateBack: () -> Unit = {},
    val onTabSelected: (Int) -> Unit = {},
    val onCallClick: () -> Unit = {},
    val onWhatsAppClick: () -> Unit = {},
    val onQuickRebookClick: () -> Unit = {},
    val onNoteChange: (String) -> Unit = {},
    val onFlushNote: () -> Unit = {}
)
