package com.uniquemakeover.admin.feature.dashboard.presentation

import com.uniquemakeover.admin.feature.dashboard.domain.model.AlertBanner
import com.uniquemakeover.admin.feature.dashboard.domain.model.DashboardKpis
import com.uniquemakeover.admin.feature.dashboard.domain.model.ScheduleItem

data class DashboardUiState(
    val isLoading: Boolean = false,
    val todayDate: String = "",
    val shopName: String = "Unique Makeover",
    val tagline: String = "Luxury Beauty Parlour",
    val logoPath: String? = null,
    val kpis: DashboardKpis = DashboardKpis(),
    val schedule: List<ScheduleItem> = emptyList(),
    val alertBanners: List<AlertBanner> = emptyList()
)
