package com.uniquemakeover.admin.core.domain.repository

import com.uniquemakeover.admin.core.domain.model.CommunicationConfig
import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.settings.domain.model.BrandingConfig
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<Settings?>
    suspend fun getSettingsSync(): Settings?
    suspend fun getOperationsConfig(): OperationsConfig
    suspend fun updateOperationsConfig(config: OperationsConfig): DataResult<Unit>
    suspend fun updateSettings(settings: Settings): DataResult<Unit>
    suspend fun getCommunicationConfig(): CommunicationConfig
    suspend fun updateCommunicationConfig(config: CommunicationConfig): DataResult<Unit>
    suspend fun getReminderTemplate(): String
    suspend fun updateReminderTemplate(template: String): DataResult<Unit>
    suspend fun getBrandingConfig(): BrandingConfig = BrandingConfig()
    suspend fun updateBrandingConfig(config: BrandingConfig): DataResult<Unit> = DataResult.Success(Unit)
    suspend fun updateLastBackup(timestamp: String): DataResult<Unit> = DataResult.Success(Unit)
}
