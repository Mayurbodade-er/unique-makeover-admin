package com.uniquemakeover.admin.feature.dashboard.domain.usecase

import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.testutil.FakeTimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetTodayScheduleUseCaseTest {

    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeRepository: FakeAppointmentRepository
    private lateinit var useCase: GetTodayScheduleUseCase

    @Before
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeTimeProvider.currentDate = "2026-11-20"
        fakeTimeProvider.currentTime24 = "11:00"
        fakeRepository = FakeAppointmentRepository()
        useCase = GetTodayScheduleUseCase(fakeRepository, fakeTimeProvider)
    }

    @Test
    fun invoke_sortsChronologicallyAndComputesPastTime() = runTest {
        val aptPast = Appointment(
            id = "1",
            name = "Rani Sharma",
            phone = "9800000001",
            serviceSnapshot = "Facial",
            serviceIds = listOf("s1"),
            rate = 1000,
            paid = 500,
            staff = "Sunita",
            date = "2026-11-20",
            time = "10:00",
            note = "",
            status = "booked",
            bookedAt = 1000L
        )
        val aptNow = Appointment(
            id = "2",
            name = "Pooja Patil",
            phone = "9800000002",
            serviceSnapshot = "Spa",
            serviceIds = listOf("s2"),
            rate = 1500,
            paid = 1500,
            staff = "Pooja",
            date = "2026-11-20",
            time = "11:00",
            note = "",
            status = "booked",
            bookedAt = 2000L
        )
        val aptFuture = Appointment(
            id = "3",
            name = "Neha Deshmukh",
            phone = "9800000003",
            serviceSnapshot = "Cleanup",
            serviceIds = listOf("s3"),
            rate = 500,
            paid = 0,
            staff = "Sunita",
            date = "2026-11-20",
            time = "14:00",
            note = "",
            status = "booked",
            bookedAt = 3000L
        )

        fakeRepository.setSchedule(listOf(aptPast, aptNow, aptFuture))

        val items = useCase().first()

        assertEquals(3, items.size)
        assertEquals("10:00", items[0].appointment.time)
        assertTrue(items[0].isPastTime)

        assertEquals("11:00", items[1].appointment.time)
        assertFalse(items[1].isPastTime)

        assertEquals("14:00", items[2].appointment.time)
        assertFalse(items[2].isPastTime)
    }

    private class FakeAppointmentRepository : AppointmentRepository {
        private var schedule: List<Appointment> = emptyList()

        fun setSchedule(list: List<Appointment>) {
            schedule = list
        }

        override fun getAllAppointments(): Flow<List<Appointment>> = flowOf(schedule)
        override fun getAppointmentsByDate(date: String): Flow<List<Appointment>> = flowOf(schedule)
        override suspend fun getAppointmentsByDateSync(date: String): List<Appointment> = schedule
        override fun getTodaySchedule(today: String): Flow<List<Appointment>> = flowOf(schedule)
        override fun getFilteredAppointments(
            status: String?,
            startDate: String?,
            endDate: String?,
            query: String
        ): Flow<List<Appointment>> = flowOf(schedule)

        override suspend fun insertAppointment(appointment: Appointment): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun updateAppointment(appointment: Appointment): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun updateStatus(id: String, status: AppointmentStatus): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun getAppointmentById(id: String): Appointment? = schedule.find { it.id == id }
        override suspend fun deleteAppointment(appointment: Appointment): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun deleteById(id: String): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun countUpcomingAppointmentsForService(serviceId: String, today: String): Int = 0
        override suspend fun count(): Int = schedule.size
    }
}
