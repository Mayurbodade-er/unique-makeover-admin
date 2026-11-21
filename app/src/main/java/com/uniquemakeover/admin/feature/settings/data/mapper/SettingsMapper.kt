package com.uniquemakeover.admin.feature.settings.data.mapper

import com.uniquemakeover.admin.core.database.entity.SettingsEntity
import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.feature.settings.domain.model.BrandingConfig

fun SettingsEntity.toDomain(): Settings {
    return Settings(
        id = id,
        shop = shop,
        tagline = tagline,
        adminPinHash = adminPinHash,
        logoPath = logoPath,
        bgPath = bgPath,
        wa = wa,
        upi = upi,
        staff = staff,
        open = open,
        close = close,
        slot = slot,
        msg = msg,
        lastBackup = lastBackup
    )
}

fun Settings.toEntity(): SettingsEntity {
    return SettingsEntity(
        id = id,
        shop = shop,
        tagline = tagline,
        adminPinHash = adminPinHash,
        logoPath = logoPath,
        bgPath = bgPath,
        wa = wa,
        upi = upi,
        staff = staff,
        open = open,
        close = close,
        slot = slot,
        msg = msg,
        lastBackup = lastBackup
    )
}

fun SettingsEntity.toOperationsConfig(): OperationsConfig {
    val staffList = staff.split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .ifEmpty { listOf("Sunita") }

    return OperationsConfig(
        openTime = open,
        closeTime = close,
        slotDurationMinutes = slot,
        staffList = staffList
    )
}

fun SettingsEntity.toBrandingConfig(): BrandingConfig {
    return BrandingConfig(
        shopName = shop,
        tagline = tagline,
        logoPath = logoPath,
        wallpaperPath = bgPath
    )
}
