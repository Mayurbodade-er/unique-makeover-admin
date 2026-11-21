package com.uniquemakeover.admin.feature.settings.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupSnapshot(
    val version: Int = 2,
    val exportedAt: String,
    val settings: SettingsBackupDto,
    val services: List<ServiceBackupDto>,
    val appointments: List<AppointmentBackupDto>,
    val clientNotes: Map<String, String>
)

@Serializable
data class SettingsBackupDto(
    val shop: String,
    val tagline: String,
    val wa: String,
    val upi: String,
    val staff: String,
    val open: String,
    val close: String,
    val slot: Int,
    val msg: String
)

@Serializable
data class ServiceBackupDto(
    val id: String,
    val name: String,
    val rate: Int,
    val imagePath: String? = null,
    val sortOrder: Int = 0
)

@Serializable
data class AppointmentBackupDto(
    val id: String,
    val name: String,
    val phone: String,
    val svc: String,
    val serviceIds: String,
    val rate: Int,
    val paid: Int,
    val staff: String,
    val date: String,
    val time: String,
    val note: String,
    val status: String,
    val reminded: String? = null,
    val bookedAt: Long
)
