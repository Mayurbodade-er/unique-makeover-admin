package com.uniquemakeover.admin.feature.appointments.data.repository

import com.uniquemakeover.admin.core.database.dao.AppointmentDao
import com.uniquemakeover.admin.core.database.entity.AppointmentEntity
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppointmentRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleEntity = AppointmentEntity(
        id = "apt_01",
        name = "Pooja Patil",
        phone = "9822001122",
        svc = "Gold Facial",
        serviceIds = listOf("s1"),
        rate = 800,
        paid = 200,
        staff = "Sunita",
        date = "2026-11-20",
        time = "10:00",
        note = "First visit",
        status = "booked",
        reminded = null,
        bookedAt = 1000L
    )

    @Test
    fun getAllAppointments_mapsEntitiesToDomainModels() = runTest(testDispatcher) {
        val fakeDao = FakeAppointmentDao(listOf(sampleEntity))
        val repository = AppointmentRepositoryImpl(fakeDao, testDispatcher)

        val list = repository.getAllAppointments().first()
        assertEquals(1, list.size)
        assertEquals("apt_01", list[0].id)
        assertEquals("Pooja Patil", list[0].name)
        assertEquals("Gold Facial", list[0].serviceSnapshot)
        assertEquals(800, list[0].rate)
    }

    @Test
    fun insertAppointment_success_delegatesToDao() = runTest(testDispatcher) {
        val fakeDao = FakeAppointmentDao(emptyList())
        val repository = AppointmentRepositoryImpl(fakeDao, testDispatcher)

        val domain = Appointment(
            id = "apt_02",
            name = "Rani Sharma",
            phone = "9876543210",
            serviceSnapshot = "Keratin Spa",
            serviceIds = listOf("s2"),
            rate = 1500,
            paid = 500,
            staff = "Pooja",
            date = "2026-11-20",
            time = "11:00",
            note = "",
            status = "booked",
            reminded = null,
            bookedAt = 2000L
        )

        val result = repository.insertAppointment(domain)
        assertTrue(result is DataResult.Success)
        assertEquals(1, fakeDao.storedEntities.size)
        assertEquals("apt_02", fakeDao.storedEntities[0].id)
    }

    private class FakeAppointmentDao(initialList: List<AppointmentEntity>) : AppointmentDao {
        val storedEntities = initialList.toMutableList()

        override fun getAllAppointments(): Flow<List<AppointmentEntity>> = flowOf(storedEntities)
        override fun getAppointmentsByDate(date: String): Flow<List<AppointmentEntity>> =
            flowOf(storedEntities.filter { it.date == date })

        override suspend fun getAppointmentsByDateSync(date: String): List<AppointmentEntity> =
            storedEntities.filter { it.date == date }

        override fun getAppointmentById(id: String): Flow<AppointmentEntity?> =
            flowOf(storedEntities.find { it.id == id })

        override suspend fun getAppointmentByIdSync(id: String): AppointmentEntity? =
            storedEntities.find { it.id == id }

        override fun getAppointmentsByPhone(phone: String): Flow<List<AppointmentEntity>> =
            flowOf(storedEntities.filter { it.phone == phone })

        override suspend fun countClashingAppointments(date: String, time: String, excludeId: String?): Int = 0

        override suspend fun insert(appointment: AppointmentEntity) {
            storedEntities.add(appointment)
        }

        override suspend fun insertAll(appointments: List<AppointmentEntity>) {
            storedEntities.addAll(appointments)
        }

        override suspend fun update(appointment: AppointmentEntity) {
            val index = storedEntities.indexOfFirst { it.id == appointment.id }
            if (index >= 0) storedEntities[index] = appointment
        }

        override suspend fun delete(appointment: AppointmentEntity) {
            storedEntities.removeAll { it.id == appointment.id }
        }

        override suspend fun count(): Int = storedEntities.size
    }
}
