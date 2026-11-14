package com.uniquemakeover.admin.feature.appointments.domain.repository

import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import kotlinx.coroutines.flow.Flow

interface AppointmentRepository {
    fun getAllAppointments(): Flow<List<Appointment>>
    fun getAppointmentsByDate(date: String): Flow<List<Appointment>>
    suspend fun getAppointmentsByDateSync(date: String): List<Appointment>
    fun getTodaySchedule(today: String): Flow<List<Appointment>>
    fun getFilteredAppointments(
        status: String?,
        startDate: String?,
        endDate: String?,
        query: String
    ): Flow<List<Appointment>>
    suspend fun insertAppointment(appointment: Appointment): DataResult<Unit>
    suspend fun updateAppointment(appointment: Appointment): DataResult<Unit>
    suspend fun deleteAppointment(appointment: Appointment): DataResult<Unit>
    suspend fun count(): Int
}
