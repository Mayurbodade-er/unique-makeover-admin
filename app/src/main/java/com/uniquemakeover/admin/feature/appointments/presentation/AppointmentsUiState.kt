package com.uniquemakeover.admin.feature.appointments.presentation

import com.uniquemakeover.admin.core.domain.model.Slot
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.model.AppointmentFilter

data class TabCounts(
    val all: Int = 0,
    val upcoming: Int = 0,
    val completed: Int = 0,
    val cancelled: Int = 0
)

data class AppointmentsUiState(
    val isLoading: Boolean = false,
    val appointments: List<Appointment> = emptyList(),
    val filter: AppointmentFilter = AppointmentFilter(),
    val tabCounts: TabCounts = TabCounts(),
    val selectedDate: String = "",
    val slots: List<Slot> = emptyList(),
    val selectedSlot: Slot? = null,
    val errorMessage: String? = null
)
