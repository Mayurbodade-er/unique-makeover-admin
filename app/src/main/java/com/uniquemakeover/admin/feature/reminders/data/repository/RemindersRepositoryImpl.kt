package com.uniquemakeover.admin.feature.reminders.data.repository

import android.database.sqlite.SQLiteException
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.database.dao.AppointmentDao
import com.uniquemakeover.admin.core.di.IoDispatcher
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.TimeProvider
import com.uniquemakeover.admin.feature.appointments.data.mapper.AppointmentMapper
import com.uniquemakeover.admin.feature.reminders.domain.model.ReminderQueueItem
import com.uniquemakeover.admin.feature.reminders.domain.repository.RemindersRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemindersRepositoryImpl @Inject constructor(
    private val appointmentDao: AppointmentDao,
    private val timeProvider: TimeProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : RemindersRepository {

    override fun getTomorrowQueue(): Flow<List<ReminderQueueItem>> {
        val tomorrow = timeProvider.tomorrowDate()
        return appointmentDao.observeTomorrowAppointments(tomorrow)
            .map { entities ->
                entities.map { entity ->
                    val domain = AppointmentMapper.toDomain(entity)
                    ReminderQueueItem(
                        appointment = domain,
                        isReminded = !domain.reminded.isNullOrBlank()
                    )
                }
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun markReminded(appointmentId: String, tomorrowDate: String): DataResult<Unit> {
        return withContext(ioDispatcher) {
            try {
                appointmentDao.updateReminded(appointmentId, tomorrowDate)
                DataResult.Success(Unit)
            } catch (e: SQLiteException) {
                DataResult.Error(
                    UiText.DynamicString(e.localizedMessage ?: "Failed to mark reminder"),
                    e
                )
            }
        }
    }

    override suspend fun undoMarkReminded(appointmentId: String): DataResult<Unit> {
        return withContext(ioDispatcher) {
            try {
                appointmentDao.updateReminded(appointmentId, null)
                DataResult.Success(Unit)
            } catch (e: SQLiteException) {
                DataResult.Error(
                    UiText.DynamicString(e.localizedMessage ?: "Failed to undo reminder"),
                    e
                )
            }
        }
    }
}
