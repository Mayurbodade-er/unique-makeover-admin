package com.uniquemakeover.admin.core.domain.model

data class Service(
    val id: String,
    val name: String,
    val rate: Int,
    val imagePath: String? = null,
    val sortOrder: Int = 0
)
