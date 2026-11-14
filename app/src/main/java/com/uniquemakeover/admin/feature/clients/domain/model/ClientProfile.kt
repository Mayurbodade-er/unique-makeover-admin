package com.uniquemakeover.admin.feature.clients.domain.model

data class ClientProfile(
    val phone: String,
    val name: String,
    val totalVisits: Int,
    val totalSpend: Int,
    val lastVisitDate: String,
    val lastServices: String
)
