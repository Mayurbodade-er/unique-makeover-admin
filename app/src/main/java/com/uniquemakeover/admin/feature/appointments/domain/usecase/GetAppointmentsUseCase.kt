package com.uniquemakeover.admin.feature.appointments.domain.usecase

import com.uniquemakeover.admin.core.util.PhoneSanitizer
import com.uniquemakeover.admin.core.util.TimeProvider
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.model.AppointmentFilter
import com.uniquemakeover.admin.feature.appointments.domain.model.FilterTab
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAppointmentsUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val timeProvider: TimeProvider,
    private val phoneSanitizer: PhoneSanitizer
) {
    operator fun invoke(filter: AppointmentFilter): Flow<List<Appointment>> {
        val today = timeProvider.todayDate()
        val (status, startDate, endDate) = when (filter.tab) {
            FilterTab.ALL -> Triple(null, filter.dateRange?.startDate, filter.dateRange?.endDate)
            FilterTab.UPCOMING -> Triple("booked", filter.dateRange?.startDate ?: today, filter.dateRange?.endDate)
            FilterTab.COMPLETED -> Triple("done", filter.dateRange?.startDate, filter.dateRange?.endDate)
            FilterTab.CANCELLED -> Triple("cancel", filter.dateRange?.startDate, filter.dateRange?.endDate)
        }
        val cleanQuery = phoneSanitizer.sanitize(filter.searchQuery) ?: filter.searchQuery.trim()
        return appointmentRepository.getFilteredAppointments(
            status = status,
            startDate = startDate,
            endDate = endDate,
            query = cleanQuery
        )
    }
}
