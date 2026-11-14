package com.uniquemakeover.admin.feature.clients.presentation.detail

import com.uniquemakeover.admin.feature.clients.domain.model.ClientHistoryItem

data class ClientDetailUiState(
    val isLoading: Boolean = false,
    val clientName: String = "",
    val phone: String = "",
    val totalSpend: Int = 0,
    val totalVisits: Int = 0,
    val historyItems: List<ClientHistoryItem> = emptyList(),
    val selectedTab: Int = 0,
    val errorMessage: String? = null
)
