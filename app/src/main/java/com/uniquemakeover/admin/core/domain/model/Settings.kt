package com.uniquemakeover.admin.core.domain.model

data class Settings(
    val id: Int = 1,
    val shop: String = "Unique Makeover",
    val tagline: String = "Luxury Beauty Parlour",
    val adminPinHash: String = "",
    val logoPath: String? = null,
    val bgPath: String? = null,
    val wa: String = "919876543210",
    val upi: String = "unique@upi",
    val staff: String = "Sunita",
    val open: String = "10:00",
    val close: String = "20:00",
    val slot: Int = 30,
    val msg: String = "",
    val lastBackup: String? = null
)
