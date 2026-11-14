package com.uniquemakeover.admin.feature.appointments.data.repository

import android.database.SQLException
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.database.dao.AppointmentDao
import com.uniquemakeover.admin.core.di.IoDispatcher
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.data.mapper.AppointmentMapper
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AppointmentRepositoryImpl @Inject constructor(
    private val appointmentDao: AppointmentDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AppointmentRepository {

    override fun getAllAppointments(): Flow<List<Appointment>> {
        return appointmentDao.getAllAppointments().map { list ->
            list.map(AppointmentMapper::toDomain)
        }
    }

    override fun getAppointmentsByDate(date: String): Flow<List<Appointment>> {
        return appointmentDao.getAppointmentsByDate(date).map { list ->
            list.map(AppointmentMapper::toDomain)
        }
    }

    override suspend fun getAppointmentsByDateSync(date: String): List<Appointment> {
        return withContext(ioDispatcher) {
            appointmentDao.getAppointmentsByDateSync(date).map(AppointmentMapper::toDomain)
        }
    }

    override suspend fun getAppointmentById(id: String): Appointment? {
        return withContext(ioDispatcher) {
            appointmentDao.getAppointmentByIdSync(id)?.let(AppointmentMapper::toDomain)
        }
    }

    override fun getTodaySchedule(today: String): Flow<List<Appointment>> {
        return appointmentDao.observeTodaySchedule(today)
            .map { entities -> entities.map(AppointmentMapper::toDomain) }
    }

    override fun getFilteredAppointments(
        status: String?,
        startDate: String?,
        endDate: String?,
        query: String
    ): Flow<List<Appointment>> {
        return appointmentDao.observeFilteredAppointments(
            status = status,
            startDate = startDate,
            endDate = endDate,
            query = query
        ).map { entities ->
            entities.map(AppointmentMapper::toDomain)
        }
    }

    override suspend fun insertAppointment(appointment: Appointment): DataResult<Unit> {
        return withContext(ioDispatcher) {
            try {
                appointmentDao.insert(AppointmentMapper.toEntity(appointment))
                DataResult.Success(Unit)
            } catch (e: SQLException) {
                DataResult.Error(UiText.DynamicString(e.localizedMessage ?: "Failed to save appointment"))
            }
        }
    }

    override suspend fun updateAppointment(appointment: Appointment): DataResult<Unit> {
        return withContext(ioDispatcher) {
            try {
                appointmentDao.update(AppointmentMapper.toEntity(appointment))
                DataResult.Success(Unit)
            } catch (e: SQLException) {
                DataResult.Error(UiText.DynamicString(e.localizedMessage ?: "Failed to update appointment"))
            }
        }
    }

    override suspend fun updateStatus(
        id: String,
        status: com.uniquemakeover.admin.core.domain.model.AppointmentStatus
    ): DataResult<Unit> {
        return withContext(ioDispatcher) {
            try {
                appointmentDao.updateStatus(id, status.value)
                DataResult.Success(Unit)
            } catch (e: SQLException) {
                DataResult.Error(UiText.DynamicString(e.localizedMessage ?: "Failed to update status"))
            }
        }
    }

    override suspend fun deleteAppointment(appointment: Appointment): DataResult<Unit> {
        return withContext(ioDispatcher) {
            try {
                appointmentDao.delete(AppointmentMapper.toEntity(appointment))
                DataResult.Success(Unit)
            } catch (e: SQLException) {
                DataResult.Error(UiText.DynamicString(e.localizedMessage ?: "Failed to delete appointment"))
            }
        }
    }

    override suspend fun deleteById(id: String): DataResult<Unit> {
        return withContext(ioDispatcher) {
            try {
                appointmentDao.deleteById(id)
                DataResult.Success(Unit)
            } catch (e: SQLException) {
                DataResult.Error(UiText.DynamicString(e.localizedMessage ?: "Failed to delete appointment"))
            }
        }
    }

    override suspend fun countUpcomingAppointmentsForService(serviceId: String, today: String): Int {
        return withContext(ioDispatcher) {
            appointmentDao.countUpcomingAppointmentsForService(serviceId, today)
        }
    }

    override suspend fun count(): Int = withContext(ioDispatcher) { appointmentDao.count() }
}
