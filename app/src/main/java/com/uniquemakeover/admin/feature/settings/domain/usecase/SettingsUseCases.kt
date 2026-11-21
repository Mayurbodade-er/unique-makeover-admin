package com.uniquemakeover.admin.feature.settings.domain.usecase

import com.uniquemakeover.admin.feature.auth.domain.usecase.ChangePinUseCase
import javax.inject.Inject

data class SettingsUseCases @Inject constructor(
    val getOperationsConfig: GetOperationsConfigUseCase,
    val updateOperations: UpdateOperationsUseCase,
    val getCommunicationConfig: GetCommunicationConfigUseCase,
    val updateCommunication: UpdateCommunicationUseCase,
    val getReminderTemplate: GetReminderTemplateUseCase,
    val updateReminderTemplate: UpdateReminderTemplateUseCase,
    val getBrandingConfig: GetBrandingConfigUseCase,
    val updateBranding: UpdateBrandingUseCase,
    val changePin: ChangePinUseCase
)
