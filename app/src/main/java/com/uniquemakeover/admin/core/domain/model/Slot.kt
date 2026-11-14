package com.uniquemakeover.admin.core.domain.model

data class Slot(
    val time24: String,
    val time12: String,
    val isAvailable: Boolean,
    val reason: UnavailableReason? = null
)

enum class UnavailableReason {
    ALREADY_BOOKED,
    PAST_TIME
}
