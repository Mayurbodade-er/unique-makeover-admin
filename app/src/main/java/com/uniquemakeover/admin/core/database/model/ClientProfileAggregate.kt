package com.uniquemakeover.admin.core.database.model

data class ClientProfileAggregate(
    val phone: String,
    val name: String,
    val totalVisits: Int,
    val totalSpend: Int,
    val lastVisitDate: String?,
    val lastServices: String?,
    val maxBookedAt: Long = 0L
)
