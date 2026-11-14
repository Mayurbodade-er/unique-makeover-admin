package com.uniquemakeover.admin.feature.appointments.data.repository

import com.uniquemakeover.admin.core.database.dao.AppointmentDao
import com.uniquemakeover.admin.core.database.entity.AppointmentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SlotRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun isClashFree_zeroClashes_returnsTrue() = runTest(testDispatcher) {
        val fakeDao = FakeAppointmentDao(clashCount = 0)
        val repository = SlotRepositoryImpl(fakeDao, testDispatcher)

        val result = repository.isClashFree("2026-11-20", "10:00")
        assertTrue(result)
    }

    @Test
    fun isClashFree_clashesExist_returnsFalse() = runTest(testDispatcher) {
        val fakeDao = FakeAppointmentDao(clashCount = 1)
        val repository = SlotRepositoryImpl(fakeDao, testDispatcher)

        val result = repository.isClashFree("2026-11-20", "10:00")
        assertFalse(result)
    }

    private class FakeAppointmentDao(private val clashCount: Int) : AppointmentDao {
        override fun getAllAppointments(): Flow<List<AppointmentEntity>> = emptyFlow()
        override fun getAppointmentsByDate(date: String): Flow<List<AppointmentEntity>> = emptyFlow()
        override suspend fun getAppointmentsByDateSync(date: String): List<AppointmentEntity> = emptyList()
        override fun getAppointmentById(id: String): Flow<AppointmentEntity?> = emptyFlow()
        override suspend fun getAppointmentByIdSync(id: String): AppointmentEntity? = null
        override fun getAppointmentsByPhone(phone: String): Flow<List<AppointmentEntity>> = emptyFlow()
        override fun observeFilteredAppointments(
            status: String?,
            startDate: String?,
            endDate: String?,
            query: String
        ): Flow<List<AppointmentEntity>> = emptyFlow()

        override suspend fun countClashingAppointments(date: String, time: String, excludeId: String?): Int {
            return clashCount
        }

        override suspend fun insert(appointment: AppointmentEntity) = Unit
        override suspend fun insertAll(appointments: List<AppointmentEntity>) = Unit
        override suspend fun update(appointment: AppointmentEntity) = Unit
        override suspend fun delete(appointment: AppointmentEntity) = Unit
        override suspend fun count(): Int = 0
    }
}
