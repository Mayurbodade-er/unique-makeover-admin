package com.uniquemakeover.admin.feature.appointments.presentation

import com.uniquemakeover.admin.core.domain.model.Slot

data class AppointmentsUiState(
    val isLoading: Boolean = false,
    val selectedDate: String = "",
    val slots: List<Slot> = emptyList(),
    val selectedSlot: Slot? = null,
    val errorMessage: String? = null
)
