package com.uniquemakeover.admin.core.domain.model

data class Payment(
    val id: String,
    val appointmentId: String,
    val amount: Int,
    val mode: PaymentMode,
    val paidAt: Long
)
