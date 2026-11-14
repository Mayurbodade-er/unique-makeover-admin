package com.uniquemakeover.admin.feature.appointments.domain.usecase

import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.PhoneSanitizer
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.model.AppointmentFilter
import com.uniquemakeover.admin.feature.appointments.domain.model.DateRange
import com.uniquemakeover.admin.feature.appointments.domain.model.FilterTab
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.testutil.FakeTimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GetAppointmentsUseCaseTest {

    private lateinit var fakeRepository: FakeAppointmentRepository
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var phoneSanitizer: PhoneSanitizer
    private lateinit var useCase: GetAppointmentsUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeAppointmentRepository()
        fakeTimeProvider = FakeTimeProvider()
        fakeTimeProvider.currentDate = "2026-11-20"
        phoneSanitizer = PhoneSanitizer()
        useCase = GetAppointmentsUseCase(
            appointmentRepository = fakeRepository,
            timeProvider = fakeTimeProvider,
            phoneSanitizer = phoneSanitizer
        )
    }

    @Test
    fun upcomingTab_mapsToBookedStatusAndTodayStartDate() = runTest {
        val filter = AppointmentFilter(tab = FilterTab.UPCOMING)
        useCase(filter).first()

        assertEquals("booked", fakeRepository.lastStatus)
        assertEquals("2026-11-20", fakeRepository.lastStartDate)
        assertNull(fakeRepository.lastEndDate)
        assertEquals("", fakeRepository.lastQuery)
    }

    @Test
    fun allTab_mapsToNullStatusAndNullDates() = runTest {
        val filter = AppointmentFilter(tab = FilterTab.ALL)
        useCase(filter).first()

        assertNull(fakeRepository.lastStatus)
        assertNull(fakeRepository.lastStartDate)
        assertNull(fakeRepository.lastEndDate)
    }

    @Test
    fun completedTab_mapsToDoneStatus() = runTest {
        val filter = AppointmentFilter(tab = FilterTab.COMPLETED)
        useCase(filter).first()

        assertEquals("done", fakeRepository.lastStatus)
    }

    @Test
    fun cancelledTab_mapsToCancelStatus() = runTest {
        val filter = AppointmentFilter(tab = FilterTab.CANCELLED)
        useCase(filter).first()

        assertEquals("cancel", fakeRepository.lastStatus)
    }

    @Test
    fun customDateRange_appliesToFilters() = runTest {
        val filter = AppointmentFilter(
            tab = FilterTab.ALL,
            dateRange = DateRange("2026-11-01", "2026-11-15")
        )
        useCase(filter).first()

        assertEquals("2026-11-01", fakeRepository.lastStartDate)
        assertEquals("2026-11-15", fakeRepository.lastEndDate)
    }

    @Test
    fun searchFormattedPhone_sanitizesToTenDigits() = runTest {
        val filter = AppointmentFilter(
            tab = FilterTab.ALL,
            searchQuery = "+91 98765 43210"
        )
        useCase(filter).first()

        assertEquals("9876543210", fakeRepository.lastQuery)
    }

    @Test
    fun searchName_trimsWhitespace() = runTest {
        val filter = AppointmentFilter(
            tab = FilterTab.ALL,
            searchQuery = "  Sharma  "
        )
        useCase(filter).first()

        assertEquals("Sharma", fakeRepository.lastQuery)
    }

    private class FakeAppointmentRepository : AppointmentRepository {
        var lastStatus: String? = null
        var lastStartDate: String? = null
        var lastEndDate: String? = null
        var lastQuery: String? = null

        override fun getAllAppointments(): Flow<List<Appointment>> = flowOf(emptyList())
        override fun getTodaySchedule(today: String): Flow<List<Appointment>> = flowOf(emptyList())
        override fun getAppointmentsByDate(date: String): Flow<List<Appointment>> = flowOf(emptyList())
        override suspend fun getAppointmentsByDateSync(date: String): List<Appointment> = emptyList()

        override fun getFilteredAppointments(
            status: String?,
            startDate: String?,
            endDate: String?,
            query: String
        ): Flow<List<Appointment>> {
            lastStatus = status
            lastStartDate = startDate
            lastEndDate = endDate
            lastQuery = query
            return flowOf(emptyList())
        }

        override suspend fun insertAppointment(appointment: Appointment): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun updateAppointment(appointment: Appointment): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun updateStatus(id: String, status: AppointmentStatus): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun getAppointmentById(id: String): Appointment? = null
        override suspend fun deleteAppointment(appointment: Appointment): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun deleteById(id: String): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun countUpcomingAppointmentsForService(serviceId: String, today: String): Int = 0
        override suspend fun count(): Int = 0
    }
}
