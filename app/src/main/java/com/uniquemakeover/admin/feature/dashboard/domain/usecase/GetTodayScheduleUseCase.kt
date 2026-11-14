package com.uniquemakeover.admin.feature.dashboard.domain.usecase

import com.uniquemakeover.admin.core.util.TimeProvider
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.dashboard.domain.model.ScheduleItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetTodayScheduleUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val timeProvider: TimeProvider
) {
    operator fun invoke(): Flow<List<ScheduleItem>> {
        val today = timeProvider.todayDate()
        val nowTime = timeProvider.nowTime24()
        return appointmentRepository.getTodaySchedule(today).map { list ->
            list.map { appointment ->
                val isPast = appointment.time < nowTime
                ScheduleItem(
                    appointment = appointment,
                    isPastTime = isPast
                )
            }
        }
    }
}
