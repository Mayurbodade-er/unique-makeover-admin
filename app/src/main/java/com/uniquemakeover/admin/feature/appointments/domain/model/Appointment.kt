package com.uniquemakeover.admin.feature.appointments.domain.model

data class Appointment(
    val id: String,
    val name: String,
    val phone: String,
    val serviceSnapshot: String,
    val serviceIds: List<String>,
    val rate: Int,
    val paid: Int,
    val staff: String,
    val date: String,
    val time: String,
    val note: String,
    val status: String,
    val reminded: String? = null,
    val bookedAt: Long
)
