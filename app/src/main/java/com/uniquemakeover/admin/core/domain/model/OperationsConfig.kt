package com.uniquemakeover.admin.core.domain.model

data class OperationsConfig(
    val openTime: String = "10:00",
    val closeTime: String = "20:00",
    val slotDurationMinutes: Int = 30,
    val staffList: List<String> = listOf("Sunita")
)
