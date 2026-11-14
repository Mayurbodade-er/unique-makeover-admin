package com.uniquemakeover.admin.feature.settings.domain.usecase

import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import javax.inject.Inject

class GetOperationsConfigUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): OperationsConfig {
        return settingsRepository.getOperationsConfig()
    }
}
