package com.uniquemakeover.admin.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val shop: String,
    val tagline: String,
    val adminPinHash: String,
    val logoPath: String? = null,
    val bgPath: String? = null,
    val wa: String,
    val upi: String,
    val staff: String,
    val open: String,
    val close: String,
    val slot: Int,
    val msg: String,
    val lastBackup: String? = null
)
