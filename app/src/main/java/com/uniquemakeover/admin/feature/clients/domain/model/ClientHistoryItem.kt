package com.uniquemakeover.admin.feature.clients.domain.model

import com.uniquemakeover.admin.core.domain.model.AppointmentStatus

data class ClientHistoryItem(
    val appointmentId: String,
    val date: String,
    val time: String,
    val formattedServices: String,
    val staff: String,
    val rate: Int,
    val paid: Int,
    val balanceDue: Int,
    val status: AppointmentStatus
)
