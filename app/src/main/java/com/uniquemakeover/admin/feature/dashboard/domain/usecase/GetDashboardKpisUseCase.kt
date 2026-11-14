package com.uniquemakeover.admin.feature.dashboard.domain.usecase

import com.uniquemakeover.admin.core.util.TimeProvider
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.dashboard.domain.model.DashboardKpis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetDashboardKpisUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val timeProvider: TimeProvider
) {
    operator fun invoke(): Flow<DashboardKpis> {
        val today = timeProvider.todayDate()
        return appointmentRepository.getAppointmentsByDate(today).map { appointments ->
            var booked = 0
            var completed = 0
            var cancelled = 0
            var volume = 0
            var revenue = 0
            var outstanding = 0

            for (apt in appointments) {
                val status = apt.status.lowercase()
                when (status) {
                    "booked" -> booked++
                    "done" -> completed++
                    "cancel", "cancelled" -> cancelled++
                }

                if (status != "cancel" && status != "cancelled") {
                    volume += apt.rate
                    revenue += apt.paid
                    val due = (apt.rate - apt.paid).coerceAtLeast(0)
                    outstanding += due
                }
            }

            DashboardKpis(
                bookedCount = booked,
                completedCount = completed,
                cancelledCount = cancelled,
                scheduledVolume = volume,
                collectedRevenue = revenue,
                balanceOutstanding = outstanding
            )
        }
    }
}
