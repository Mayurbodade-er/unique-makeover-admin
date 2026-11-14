package com.uniquemakeover.admin.core.domain.model

enum class PaymentMode(val value: String) {
    CASH("cash"),
    UPI("upi");

    companion object {
        fun fromValue(value: String): PaymentMode {
            return when (value.lowercase()) {
                "upi" -> UPI
                else -> CASH
            }
        }
    }
}
