package com.uniquemakeover.admin.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "client_notes")
data class ClientNoteEntity(
    @PrimaryKey
    val phone: String,
    val note: String,
    val updatedAt: Long
)
