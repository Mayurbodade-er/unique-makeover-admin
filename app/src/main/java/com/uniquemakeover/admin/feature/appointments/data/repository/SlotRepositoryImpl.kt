package com.uniquemakeover.admin.feature.appointments.data.repository

import com.uniquemakeover.admin.core.database.dao.AppointmentDao
import com.uniquemakeover.admin.core.di.IoDispatcher
import com.uniquemakeover.admin.core.domain.model.Slot
import com.uniquemakeover.admin.feature.appointments.domain.repository.SlotRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SlotRepositoryImpl @Inject constructor(
    private val appointmentDao: AppointmentDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SlotRepository {

    override suspend fun isClashFree(date: String, time: String, excludeId: String?): Boolean {
        return withContext(ioDispatcher) {
            val count = if (excludeId != null) {
                appointmentDao.countClashingAppointmentsExcludingId(date, time, excludeId)
            } else {
                appointmentDao.countClashingAppointments(date, time, null)
            }
            count == 0
        }
    }

    override suspend fun getAvailableSlots(date: String): List<Slot> {
        return emptyList()
    }
}
