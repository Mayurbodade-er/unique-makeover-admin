package com.uniquemakeover.admin.feature.settings.data.repository

import android.database.sqlite.SQLiteException
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.database.dao.SettingsDao
import com.uniquemakeover.admin.core.database.entity.SettingsEntity
import com.uniquemakeover.admin.core.di.IoDispatcher
import com.uniquemakeover.admin.core.domain.model.CommunicationConfig
import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.reminders.domain.model.TemplateTokens
import com.uniquemakeover.admin.feature.settings.data.mapper.toBrandingConfig
import com.uniquemakeover.admin.feature.settings.data.mapper.toDomain
import com.uniquemakeover.admin.feature.settings.data.mapper.toEntity
import com.uniquemakeover.admin.feature.settings.data.mapper.toOperationsConfig
import com.uniquemakeover.admin.feature.settings.domain.model.BrandingConfig
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

    override suspend fun getCommunicationConfig(): CommunicationConfig {
        return withContext(ioDispatcher) {
            val entity = settingsDao.getSettingsSync() ?: createDefaultSettingsEntity()
            CommunicationConfig(
                whatsappNumber = entity.wa,
                merchantUpiId = entity.upi
            )
        }
    }

    override suspend fun updateCommunicationConfig(config: CommunicationConfig): DataResult<Unit> {
        return withContext(ioDispatcher) {
            try {
                val current = settingsDao.getSettingsSync() ?: createDefaultSettingsEntity()
                val updated = current.copy(
                    wa = config.whatsappNumber,
                    upi = config.merchantUpiId
                )
                settingsDao.upsert(updated)
                DataResult.Success(Unit)
            } catch (e: SQLiteException) {
                DataResult.Error(
                    UiText.DynamicString(e.localizedMessage ?: "Failed to save communication settings"),
                    e
                )
            }
        }
    }

    override suspend fun getReminderTemplate(): String {
        return withContext(ioDispatcher) {
            val entity = settingsDao.getSettingsSync() ?: createDefaultSettingsEntity()
            if (entity.msg.isNotBlank() && entity.msg.contains("{")) {
                entity.msg
            } else {
                TemplateTokens.DEFAULT_TEMPLATE
            }
        }
    }

    override suspend fun updateReminderTemplate(template: String): DataResult<Unit> {
        return withContext(ioDispatcher) {
            try {
                val current = settingsDao.getSettingsSync() ?: createDefaultSettingsEntity()
                val updated = current.copy(msg = template)
                settingsDao.upsert(updated)
                DataResult.Success(Unit)
            } catch (e: SQLiteException) {
                DataResult.Error(
                    UiText.DynamicString(e.localizedMessage ?: "Failed to save reminder template"),
                    e
                )
            }
        }
    }

    override suspend fun getBrandingConfig(): BrandingConfig {
        return withContext(ioDispatcher) {
            val entity = settingsDao.getSettingsSync() ?: createDefaultSettingsEntity()
            entity.toBrandingConfig()
        }
    }

    override suspend fun updateBrandingConfig(config: BrandingConfig): DataResult<Unit> {
        return withContext(ioDispatcher) {
            try {
                val current = settingsDao.getSettingsSync() ?: createDefaultSettingsEntity()
                val updated = current.copy(
                    shop = config.shopName,
                    tagline = config.tagline,
                    logoPath = config.logoPath,
                    bgPath = config.wallpaperPath
                )
                settingsDao.upsert(updated)
                DataResult.Success(Unit)
            } catch (e: SQLiteException) {
                DataResult.Error(
                    UiText.DynamicString(e.localizedMessage ?: "Failed to save branding settings"),
                    e
                )
            }
        }
    }

    override suspend fun updateLastBackup(timestamp: String): DataResult<Unit> {
        return withContext(ioDispatcher) {
            try {
                val current = settingsDao.getSettingsSync() ?: createDefaultSettingsEntity()
                val updated = current.copy(lastBackup = timestamp)
                settingsDao.upsert(updated)
                DataResult.Success(Unit)
            } catch (e: SQLiteException) {
                DataResult.Error(
                    UiText.DynamicString(e.localizedMessage ?: "Failed to update last backup"),
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
            msg = TemplateTokens.DEFAULT_TEMPLATE
        )
    }
}
