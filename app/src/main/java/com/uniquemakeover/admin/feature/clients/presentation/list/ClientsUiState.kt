package com.uniquemakeover.admin.feature.clients.presentation.list

import com.uniquemakeover.admin.feature.clients.domain.model.ClientProfile

data class ClientsUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val clients: List<ClientProfile> = emptyList(),
    val errorMessage: String? = null
)
