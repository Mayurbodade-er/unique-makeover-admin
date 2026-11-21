package com.uniquemakeover.admin.feature.settings.domain.usecase

import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.feature.settings.domain.model.BrandingConfig
import javax.inject.Inject

class GetBrandingConfigUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): BrandingConfig {
        return settingsRepository.getBrandingConfig()
    }
}
