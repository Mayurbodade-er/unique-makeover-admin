package com.uniquemakeover.admin.feature.dashboard.domain.usecase

import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.testutil.FakeTimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetDashboardKpisUseCaseTest {

    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeRepository: FakeAppointmentRepository
    private lateinit var useCase: GetDashboardKpisUseCase

    @Before
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider()
        fakeTimeProvider.currentDate = "2026-11-20"
        fakeRepository = FakeAppointmentRepository()
        useCase = GetDashboardKpisUseCase(
            appointmentRepository = fakeRepository,
            timeProvider = fakeTimeProvider
        )
    }

    @Test
    fun computeKpis_calculatesMetricsAndExcludesCancelledAmounts() = runTest {
        val aptA = Appointment(
            id = "a",
            name = "Apt A",
            phone = "9800000001",
            serviceSnapshot = "Facial",
            serviceIds = listOf("s1"),
            rate = 1000,
            paid = 1000,
            staff = "Sunita",
            date = "2026-11-20",
            time = "10:00",
            note = "",
            status = "done",
            bookedAt = 1000L
        )
        val aptB = Appointment(
            id = "b",
            name = "Apt B",
            phone = "9800000002",
            serviceSnapshot = "Haircut",
            serviceIds = listOf("s2"),
            rate = 2000,
            paid = 500,
            staff = "Sunita",
            date = "2026-11-20",
            time = "11:00",
            note = "",
            status = "booked",
            bookedAt = 2000L
        )
        val aptC = Appointment(
            id = "c",
            name = "Apt C",
            phone = "9800000003",
            serviceSnapshot = "Massage",
            serviceIds = listOf("s3"),
            rate = 1500,
            paid = 0,
            staff = "Sunita",
            date = "2026-11-20",
            time = "12:00",
            note = "",
            status = "cancel",
            bookedAt = 3000L
        )

        fakeRepository.setAppointmentsForToday(listOf(aptA, aptB, aptC))

        val kpis = useCase().first()

        assertEquals(1, kpis.bookedCount)
        assertEquals(1, kpis.completedCount)
        assertEquals(1, kpis.cancelledCount)
        assertEquals(3000, kpis.scheduledVolume)
        assertEquals(1500, kpis.collectedRevenue)
        assertEquals(1500, kpis.balanceOutstanding)
    }

    @Test
    fun computeKpis_zeroAppointments_returnsAllZeros() = runTest {
        fakeRepository.setAppointmentsForToday(emptyList())

        val kpis = useCase().first()

        assertEquals(0, kpis.bookedCount)
        assertEquals(0, kpis.completedCount)
        assertEquals(0, kpis.cancelledCount)
        assertEquals(0, kpis.scheduledVolume)
        assertEquals(0, kpis.collectedRevenue)
        assertEquals(0, kpis.balanceOutstanding)
    }

    @Test
    fun computeKpis_overpayment_clampsDueToZero() = runTest {
        val overpaidApt = Appointment(
            id = "d",
            name = "Apt D",
            phone = "9800000004",
            serviceSnapshot = "Bridal",
            serviceIds = listOf("s4"),
            rate = 1000,
            paid = 1200,
            staff = "Sunita",
            date = "2026-11-20",
            time = "14:00",
            note = "",
            status = "done",
            bookedAt = 4000L
        )

        fakeRepository.setAppointmentsForToday(listOf(overpaidApt))

        val kpis = useCase().first()

        assertEquals(1000, kpis.scheduledVolume)
        assertEquals(1200, kpis.collectedRevenue)
        assertEquals(0, kpis.balanceOutstanding)
    }

    private class FakeAppointmentRepository : AppointmentRepository {
        private var todayAppointments: List<Appointment> = emptyList()

        fun setAppointmentsForToday(list: List<Appointment>) {
            todayAppointments = list
        }

        override fun getAllAppointments(): Flow<List<Appointment>> = flowOf(todayAppointments)
        override fun getTodaySchedule(today: String): Flow<List<Appointment>> = flowOf(todayAppointments)
        override fun getAppointmentsByDate(date: String): Flow<List<Appointment>> = flowOf(todayAppointments)
        override suspend fun getAppointmentsByDateSync(date: String): List<Appointment> = todayAppointments
        override fun getFilteredAppointments(
            status: String?,
            startDate: String?,
            endDate: String?,
            query: String
        ): Flow<List<Appointment>> = flowOf(todayAppointments)

        override suspend fun insertAppointment(appointment: Appointment): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun updateAppointment(appointment: Appointment): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun deleteAppointment(appointment: Appointment): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun count(): Int = todayAppointments.size
    }
}
