package com.uniquemakeover.admin.feature.appointments.domain.usecase

import javax.inject.Inject

data class AppointmentOperations @Inject constructor(
    val markDone: MarkAppointmentDoneUseCase,
    val cancel: CancelAppointmentUseCase,
    val undo: UndoAppointmentStatusUseCase,
    val delete: DeleteAppointmentUseCase
)
