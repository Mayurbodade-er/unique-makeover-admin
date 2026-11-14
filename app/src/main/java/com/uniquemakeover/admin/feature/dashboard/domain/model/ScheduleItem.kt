package com.uniquemakeover.admin.feature.dashboard.domain.model

import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment

data class ScheduleItem(
    val appointment: Appointment,
    val isPastTime: Boolean = false
)
