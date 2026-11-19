package com.uniquemakeover.admin.feature.appointments.presentation.booking

import com.uniquemakeover.admin.feature.appointments.domain.usecase.BookAppointmentUseCase
import com.uniquemakeover.admin.feature.appointments.domain.usecase.EditAppointmentUseCase
import com.uniquemakeover.admin.feature.clients.domain.usecase.BuildRebookDraftUseCase
import javax.inject.Inject

data class BookingOperations @Inject constructor(
    val bookAppointment: BookAppointmentUseCase,
    val editAppointment: EditAppointmentUseCase,
    val buildRebookDraft: BuildRebookDraftUseCase
)
