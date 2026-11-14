package com.uniquemakeover.admin.feature.services.presentation

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.Service

data class ServicesUiState(
    val isLoading: Boolean = true,
    val services: List<Service> = emptyList(),
    val errorMessage: UiText? = null,
    val isEditorOpen: Boolean = false,
    val editingService: Service? = null,
    val editorName: String = "",
    val editorRate: String = "",
    val editorError: UiText? = null,
    val isSaving: Boolean = false,
    val deleteTarget: Service? = null,
    val upcomingCountForDelete: Int = 0
) {
    val isEditMode: Boolean get() = editingService != null
}
