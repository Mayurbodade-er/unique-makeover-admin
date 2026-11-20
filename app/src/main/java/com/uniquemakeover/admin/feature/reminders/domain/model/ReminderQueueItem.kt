package com.uniquemakeover.admin.feature.reminders.domain.model

import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment

data class ReminderQueueItem(
    val appointment: Appointment,
    val isReminded: Boolean
)
