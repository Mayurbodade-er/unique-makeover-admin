package com.uniquemakeover.admin.feature.services.presentation

import com.uniquemakeover.admin.core.domain.model.Service

data class ServiceEditorActions(
    val onNameChange: (String) -> Unit,
    val onRateChange: (String) -> Unit,
    val onSave: () -> Unit,
    val onDelete: (Service) -> Unit,
    val onDismiss: () -> Unit
)
