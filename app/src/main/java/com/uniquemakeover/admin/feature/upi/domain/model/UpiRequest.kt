package com.uniquemakeover.admin.feature.upi.domain.model

data class UpiRequest(
    val upiId: String,
    val payeeName: String,
    val amount: Int,
    val transactionNote: String
)
