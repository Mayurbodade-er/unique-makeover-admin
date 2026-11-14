package com.uniquemakeover.admin.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "appointments",
    indices = [
        Index(value = ["date"], name = "index_appointments_date"),
        Index(value = ["phone"], name = "index_appointments_phone"),
        Index(value = ["date", "time"], name = "index_appointments_date_time")
    ]
)
data class AppointmentEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val phone: String,
    val svc: String,
    val serviceIds: List<String>,
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
