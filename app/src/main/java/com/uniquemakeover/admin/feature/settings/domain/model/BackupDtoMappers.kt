package com.uniquemakeover.admin.feature.settings.domain.model

import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val backupJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

fun Settings.toBackupDto(): SettingsBackupDto {
    return SettingsBackupDto(
        shop = shop,
        tagline = tagline,
        wa = wa,
        upi = upi,
        staff = staff,
        open = open,
        close = close,
        slot = slot,
        msg = msg
    )
}

fun Service.toBackupDto(): ServiceBackupDto {
    return ServiceBackupDto(
        id = id,
        name = name,
        rate = rate,
        imagePath = imagePath,
        sortOrder = sortOrder
    )
}

fun Appointment.toBackupDto(): AppointmentBackupDto {
    return AppointmentBackupDto(
        id = id,
        name = name,
        phone = phone,
        svc = serviceSnapshot,
        serviceIds = backupJson.encodeToString(serviceIds),
        rate = rate,
        paid = paid,
        staff = staff,
        date = date,
        time = time,
        note = note,
        status = status,
        reminded = reminded,
        bookedAt = bookedAt
    )
}
