package com.uniquemakeover.admin.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val rate: Int,
    val imagePath: String? = null,
    val sortOrder: Int = 0
)
