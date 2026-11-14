package com.uniquemakeover.admin.feature.dashboard.domain.model

data class DashboardKpis(
    val bookedCount: Int = 0,
    val completedCount: Int = 0,
    val cancelledCount: Int = 0,
    val scheduledVolume: Int = 0,
    val collectedRevenue: Int = 0,
    val balanceOutstanding: Int = 0
)
