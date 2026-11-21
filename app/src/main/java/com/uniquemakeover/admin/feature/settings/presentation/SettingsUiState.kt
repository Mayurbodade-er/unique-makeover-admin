package com.uniquemakeover.admin.feature.settings.presentation

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.CommunicationConfig
import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.feature.settings.domain.model.BrandingConfig

data class SettingsUiState(
    val isLoading: Boolean = false,
    val brandingConfig: BrandingConfig = BrandingConfig(),
    val operationsConfig: OperationsConfig = OperationsConfig(),
    val communicationConfig: CommunicationConfig = CommunicationConfig(),
    val reminderTemplate: String = "",
    val errorMessage: UiText? = null,
    val commsErrorMessage: UiText? = null,
    val templateErrorMessage: UiText? = null,
    val brandingErrorMessage: UiText? = null,
    val pinErrorMessage: UiText? = null,
    val successMessage: UiText? = null,
    val isChangePinSuccess: Boolean = false
)
