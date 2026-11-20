package com.uniquemakeover.admin.feature.upi.domain.model

sealed interface PaymentPreset {
    data object BalanceDue : PaymentPreset
    data object Advance30 : PaymentPreset
    data object FullPayment : PaymentPreset
    data class Custom(val customAmount: Int = 0) : PaymentPreset
}
