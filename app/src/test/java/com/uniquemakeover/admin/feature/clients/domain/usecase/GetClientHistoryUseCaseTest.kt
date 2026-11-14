package com.uniquemakeover.admin.feature.clients.domain.usecase

import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetClientHistoryUseCaseTest {

    @Test
    fun invoke_sortsReverseChronologically_andCalculatesBalanceDue() = runTest {
        val olderApt = fakeApt("apt_1", "2026-10-15", "10:00").copy(
            rate = 1200,
            paid = 500,
            status = "booked"
        )
        val newerApt = fakeApt("apt_2", "2026-11-20", "14:30").copy(
            rate = 800,
            paid = 800,
            status = "done"
        )
        val sameDayEarlierApt = fakeApt("apt_3", "2026-11-20", "11:00").copy(
            rate = 300,
            paid = 300,
            status = "done"
        )

        val fakeRepo = FakeAppointmentRepo(listOf(olderApt, newerApt, sameDayEarlierApt))
        val useCase = GetClientHistoryUseCase(fakeRepo)

        val result = useCase("9822001122").first()

        assertEquals(3, result.size)
        // 1st: 2026-11-20 14:30
        assertEquals("apt_2", result[0].appointmentId)
        assertEquals("2026-11-20", result[0].date)
        assertEquals("14:30", result[0].time)
        assertEquals(800, result[0].rate)
        assertEquals(800, result[0].paid)
        assertEquals(0, result[0].balanceDue)
        assertEquals(AppointmentStatus.DONE, result[0].status)

        // 2nd: 2026-11-20 11:00
        assertEquals("apt_3", result[1].appointmentId)

        // 3rd: 2026-10-15 10:00
        assertEquals("apt_1", result[2].appointmentId)
        assertEquals(1200, result[2].rate)
        assertEquals(500, result[2].paid)
        assertEquals(700, result[2].balanceDue)
        assertEquals(AppointmentStatus.BOOKED, result[2].status)
    }

    @Test
    fun invoke_whenCancelled_setsBalanceDueToZero() = runTest {
        val cancelledApt = fakeApt("apt_c", "2026-11-10", "12:00").copy(
            rate = 2000,
            paid = 0,
            status = "cancel"
        )
        val fakeRepo = FakeAppointmentRepo(listOf(cancelledApt))
        val useCase = GetClientHistoryUseCase(fakeRepo)

        val result = useCase("9822001122").first()

        assertEquals(1, result.size)
        assertEquals(AppointmentStatus.CANCEL, result[0].status)
        assertEquals(0, result[0].balanceDue)
    }

    private fun fakeApt(
        id: String,
        date: String,
        time: String
    ): Appointment {
        return Appointment(
            id = id,
            name = "Neha",
            phone = "9822001122",
            serviceSnapshot = "Facial",
            serviceIds = listOf("s1"),
            rate = 1000,
            paid = 1000,
            staff = "Sunita",
            date = date,
            time = time,
            note = "",
            status = "done",
            reminded = null,
            bookedAt = 1000L
        )
    }

    private class FakeAppointmentRepo(
        private val list: List<Appointment>
    ) : AppointmentRepository {
        override fun getAllAppointments(): Flow<List<Appointment>> = flowOf(list)
        override fun getAppointmentsByPhone(phone: String): Flow<List<Appointment>> = flowOf(list)
        override fun getAppointmentsByDate(date: String): Flow<List<Appointment>> = flowOf(list)
        override suspend fun getAppointmentsByDateSync(date: String): List<Appointment> = list
        override suspend fun getAppointmentById(id: String): Appointment? = list.find { it.id == id }
        override fun getTodaySchedule(today: String): Flow<List<Appointment>> = flowOf(list)
        override fun getFilteredAppointments(
            status: String?,
            startDate: String?,
            endDate: String?,
            query: String
        ): Flow<List<Appointment>> = flowOf(list)
        override suspend fun insertAppointment(appointment: Appointment): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun updateAppointment(appointment: Appointment): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun updateStatus(
            id: String,
            status: AppointmentStatus
        ): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun deleteAppointment(appointment: Appointment): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun deleteById(id: String): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun countUpcomingAppointmentsForService(serviceId: String, today: String): Int = 0
        override suspend fun count(): Int = list.size
    }
}
