package com.uniquemakeover.admin.core.domain.repository

import com.uniquemakeover.admin.core.domain.model.CommunicationConfig
import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.core.domain.result.DataResult
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<Settings?>
    suspend fun getSettingsSync(): Settings?
    suspend fun getOperationsConfig(): OperationsConfig
    suspend fun updateOperationsConfig(config: OperationsConfig): DataResult<Unit>
    suspend fun updateSettings(settings: Settings): DataResult<Unit>
    suspend fun getCommunicationConfig(): CommunicationConfig
    suspend fun updateCommunicationConfig(config: CommunicationConfig): DataResult<Unit>
}
