package com.uniquemakeover.admin.feature.appointments.presentation

import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.model.DateRange
import com.uniquemakeover.admin.feature.appointments.domain.model.FilterTab

data class AppointmentsActions(
    val onTabSelected: (FilterTab) -> Unit = {},
    val onSearchQueryChange: (String) -> Unit = {},
    val onDateRangeSelected: (DateRange?) -> Unit = {},
    val onClearFilters: () -> Unit = {},
    val onOpenBooking: () -> Unit = {},
    val onAppointmentClick: (Appointment) -> Unit = {}
)
