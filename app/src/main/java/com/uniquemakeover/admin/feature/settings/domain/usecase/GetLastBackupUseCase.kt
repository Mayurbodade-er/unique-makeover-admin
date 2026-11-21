package com.uniquemakeover.admin.feature.settings.domain.usecase

import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import javax.inject.Inject

class GetLastBackupUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): String? {
        return settingsRepository.getSettingsSync()?.lastBackup
    }
}
