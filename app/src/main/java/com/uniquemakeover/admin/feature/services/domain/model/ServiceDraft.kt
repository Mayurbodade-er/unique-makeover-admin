package com.uniquemakeover.admin.feature.services.domain.model

data class ServiceDraft(
    val id: String? = null,
    val name: String,
    val rate: Int,
    val imagePath: String? = null,
    val sortOrder: Int = 0
)
