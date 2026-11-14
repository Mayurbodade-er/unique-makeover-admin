package com.uniquemakeover.admin.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    indices = [
        Index(value = ["appointmentId"], name = "index_payments_appointmentId")
    ]
)
data class PaymentEntity(
    @PrimaryKey
    val id: String,
    val appointmentId: String,
    val amount: Int,
    val mode: String,
    val paidAt: Long
)
