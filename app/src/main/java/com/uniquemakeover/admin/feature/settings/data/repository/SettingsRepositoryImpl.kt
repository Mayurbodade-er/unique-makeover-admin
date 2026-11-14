package com.uniquemakeover.admin.feature.settings.data.repository

import android.database.sqlite.SQLiteException
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.database.dao.SettingsDao
import com.uniquemakeover.admin.core.database.entity.SettingsEntity
import com.uniquemakeover.admin.core.di.IoDispatcher
import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.settings.data.mapper.toDomain
import com.uniquemakeover.admin.feature.settings.data.mapper.toEntity
import com.uniquemakeover.admin.feature.settings.data.mapper.toOperationsConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SettingsRepository {

    override fun getSettings(): Flow<Settings?> {
        return settingsDao.getSettings()
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)
    }

    override suspend fun getSettingsSync(): Settings? {
        return withContext(ioDispatcher) {
            settingsDao.getSettingsSync()?.toDomain()
        }
    }

    override suspend fun getOperationsConfig(): OperationsConfig {
        return withContext(ioDispatcher) {
            val entity = settingsDao.getSettingsSync()
            if (entity != null) {
                entity.toOperationsConfig()
            } else {
                val defaultEntity = createDefaultSettingsEntity()
                settingsDao.insert(defaultEntity)
                defaultEntity.toOperationsConfig()
            }
        }
    }

    override suspend fun updateOperationsConfig(config: OperationsConfig): DataResult<Unit> {
        return withContext(ioDispatcher) {
            try {
                val current = settingsDao.getSettingsSync() ?: createDefaultSettingsEntity()
                val updated = current.copy(
                    open = config.openTime,
                    close = config.closeTime,
                    slot = config.slotDurationMinutes,
                    staff = config.staffList.joinToString(",")
                )
                settingsDao.upsert(updated)
                DataResult.Success(Unit)
            } catch (e: SQLiteException) {
                DataResult.Error(
                    UiText.DynamicString(e.localizedMessage ?: "Failed to save operations settings"),
                    e
                )
            }
        }
    }

    override suspend fun updateSettings(settings: Settings): DataResult<Unit> {
        return withContext(ioDispatcher) {
            try {
                settingsDao.upsert(settings.toEntity())
                DataResult.Success(Unit)
            } catch (e: SQLiteException) {
                DataResult.Error(
                    UiText.DynamicString(e.localizedMessage ?: "Failed to save settings"),
                    e
                )
            }
        }
    }

    private fun createDefaultSettingsEntity(): SettingsEntity {
        return SettingsEntity(
            id = 1,
            shop = "Unique Makeover",
            tagline = "Luxury Beauty Parlour",
            adminPinHash = "",
            logoPath = null,
            bgPath = null,
            wa = "919876543210",
            upi = "unique@upi",
            staff = "Sunita",
            open = "10:00",
            close = "20:00",
            slot = 30,
            msg = "Reminder: Your appointment at Unique Makeover is scheduled for tomorrow."
        )
    }
}
