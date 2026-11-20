package com.uniquemakeover.admin.feature.settings.presentation

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.CommunicationConfig
import com.uniquemakeover.admin.core.domain.model.OperationsConfig

data class SettingsUiState(
    val isLoading: Boolean = false,
    val operationsConfig: OperationsConfig = OperationsConfig(),
    val communicationConfig: CommunicationConfig = CommunicationConfig(),
    val reminderTemplate: String = "",
    val errorMessage: UiText? = null,
    val commsErrorMessage: UiText? = null,
    val templateErrorMessage: UiText? = null,
    val successMessage: UiText? = null
)
