package com.uniquemakeover.admin.feature.settings.domain.usecase

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import javax.inject.Inject

class UpdateReminderTemplateUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(template: String): DataResult<Unit> {
        val trimmed = template.trim()
        if (trimmed.isEmpty()) {
            return DataResult.Error(
                UiText.DynamicString("Template cannot be empty")
            )
        }
        return settingsRepository.updateReminderTemplate(trimmed)
    }
}
