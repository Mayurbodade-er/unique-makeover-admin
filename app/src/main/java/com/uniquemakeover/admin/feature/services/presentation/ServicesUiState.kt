package com.uniquemakeover.admin.feature.services.presentation

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.Service

data class ServicesUiState(
    val isLoading: Boolean = true,
    val services: List<Service> = emptyList(),
    val errorMessage: UiText? = null
)
