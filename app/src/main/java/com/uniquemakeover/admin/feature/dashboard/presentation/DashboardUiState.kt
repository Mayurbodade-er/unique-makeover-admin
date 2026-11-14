package com.uniquemakeover.admin.feature.dashboard.presentation

import com.uniquemakeover.admin.feature.dashboard.domain.model.DashboardKpis

data class DashboardUiState(
    val isLoading: Boolean = false,
    val todayDate: String = "",
    val kpis: DashboardKpis = DashboardKpis()
)
