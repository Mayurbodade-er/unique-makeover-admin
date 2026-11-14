package com.uniquemakeover.admin.feature.appointments.domain.model

import com.uniquemakeover.admin.core.common.UiText

sealed interface BookingValidationResult {
    data object Valid : BookingValidationResult
    data class Invalid(
        val nameError: UiText? = null,
        val phoneError: UiText? = null,
        val serviceError: UiText? = null,
        val slotError: UiText? = null,
        val financialError: UiText? = null
    ) : BookingValidationResult
}
