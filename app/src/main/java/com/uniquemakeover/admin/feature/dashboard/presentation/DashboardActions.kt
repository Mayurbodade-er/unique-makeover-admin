package com.uniquemakeover.admin.feature.dashboard.presentation

import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.dashboard.domain.model.AlertBanner
import com.uniquemakeover.admin.feature.dashboard.domain.model.BannerDeepLinkTarget
import com.uniquemakeover.admin.feature.dashboard.presentation.components.ScheduleCardActions

data class DashboardAlertActions(
    val onActionClick: (AlertBanner) -> Unit = {},
    val onDismissClick: (AlertBanner) -> Unit = {}
)

data class DashboardNavCallbacks(
    val onNavigateToUpiQr: (appointment: Appointment, amount: Int) -> Unit = { _, _ -> },
    val onNavigateToDeepLink: (BannerDeepLinkTarget) -> Unit = {}
)

data class DashboardActions(
    val schedule: ScheduleCardActions = ScheduleCardActions(),
    val alerts: DashboardAlertActions = DashboardAlertActions(),
    val onOpenBooking: () -> Unit = {}
)
