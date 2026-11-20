package com.uniquemakeover.admin.feature.settings.domain.usecase

import com.uniquemakeover.admin.core.domain.model.CommunicationConfig
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import javax.inject.Inject

class GetCommunicationConfigUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): CommunicationConfig {
        return settingsRepository.getCommunicationConfig()
    }
}
