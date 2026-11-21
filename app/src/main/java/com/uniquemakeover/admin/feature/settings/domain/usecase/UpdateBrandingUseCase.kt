package com.uniquemakeover.admin.feature.settings.domain.usecase

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.settings.domain.model.BrandingConfig
import javax.inject.Inject

class UpdateBrandingUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(config: BrandingConfig): DataResult<Unit> {
        val trimmedName = config.shopName.trim()
        if (trimmedName.isEmpty()) {
            return DataResult.Error(UiText.DynamicString("Salon name cannot be empty"))
        }
        val sanitizedConfig = config.copy(
            shopName = trimmedName,
            tagline = config.tagline.trim()
        )
        return settingsRepository.updateBrandingConfig(sanitizedConfig)
    }
}
