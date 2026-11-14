package com.uniquemakeover.admin.core.domain.model

import com.uniquemakeover.admin.core.common.UiText

enum class AppointmentStatus(val value: String) {
    BOOKED("booked"),
    DONE("done"),
    CANCEL("cancel");

    companion object {
        fun fromValue(value: String): AppointmentStatus {
            return when (value.lowercase()) {
                "done" -> DONE
                "cancel", "cancelled" -> CANCEL
                else -> BOOKED
            }
        }
    }
}

sealed interface StatusUpdateResult {
    data object Success : StatusUpdateResult
    data class SlotConflict(val message: UiText) : StatusUpdateResult
    data class InvalidTransition(val message: UiText) : StatusUpdateResult
    data class NotFound(val message: UiText) : StatusUpdateResult
}
