package com.uniquemakeover.admin.feature.dashboard.presentation

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.AppointmentStatus

sealed interface DashboardUiEffect {
    data class ShowUndoSnackbar(
        val message: UiText,
        val actionLabel: String = "Undo",
        val appointmentId: String,
        val previousStatus: AppointmentStatus
    ) : DashboardUiEffect

    data class ShowMessage(val message: UiText) : DashboardUiEffect
}
