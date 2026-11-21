package com.uniquemakeover.admin.feature.settings.domain.usecase

import javax.inject.Inject

data class SettingsUseCases @Inject constructor(
    val getOperationsConfig: GetOperationsConfigUseCase,
    val updateOperations: UpdateOperationsUseCase,
    val getCommunicationConfig: GetCommunicationConfigUseCase,
    val updateCommunication: UpdateCommunicationUseCase,
    val getReminderTemplate: GetReminderTemplateUseCase,
    val updateReminderTemplate: UpdateReminderTemplateUseCase,
    val getBrandingConfig: GetBrandingConfigUseCase,
    val updateBranding: UpdateBrandingUseCase
)
