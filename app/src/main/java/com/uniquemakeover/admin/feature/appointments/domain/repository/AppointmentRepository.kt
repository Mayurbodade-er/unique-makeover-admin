package com.uniquemakeover.admin.feature.appointments.domain.repository

import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface AppointmentRepository {
    fun getAllAppointments(): Flow<List<Appointment>>
    fun getAppointmentsByPhone(phone: String): Flow<List<Appointment>> = emptyFlow()
    fun getAppointmentsByDate(date: String): Flow<List<Appointment>>
    suspend fun getAppointmentsByDateSync(date: String): List<Appointment>
    suspend fun getAppointmentById(id: String): Appointment?
    fun getTodaySchedule(today: String): Flow<List<Appointment>>
    fun getFilteredAppointments(
        status: String?,
        startDate: String?,
        endDate: String?,
        query: String
    ): Flow<List<Appointment>>
    suspend fun insertAppointment(appointment: Appointment): DataResult<Unit>
    suspend fun updateAppointment(appointment: Appointment): DataResult<Unit>
    suspend fun updateStatus(id: String, status: AppointmentStatus): DataResult<Unit>
    suspend fun deleteAppointment(appointment: Appointment): DataResult<Unit>
    suspend fun deleteById(id: String): DataResult<Unit>
    suspend fun countUpcomingAppointmentsForService(serviceId: String, today: String): Int
    suspend fun count(): Int
}
