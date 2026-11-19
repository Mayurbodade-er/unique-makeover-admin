package com.uniquemakeover.admin.feature.clients.domain.usecase

import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.domain.repository.ServiceRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildRebookDraftUseCaseTest {

    private val activeCatalog = listOf(
        Service(id = "s1", name = "Keratin Spa", rate = 1000),
        Service(id = "s2", name = "Threading", rate = 150),
        Service(id = "s3", name = "Hydra Facial", rate = 1500)
    )

    private val fakeServiceRepo = object : ServiceRepository {
        override fun getServices(): Flow<List<Service>> = flowOf(activeCatalog)
        override suspend fun getServiceById(id: String): Service? = activeCatalog.find { it.id == id }
        override suspend fun saveService(service: Service): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun deleteService(id: String): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun countDuplicateNames(name: String, excludeId: String): Int = 0
    }

    @Test
    fun invoke_withCompletedVisit_preFillsNamePhoneAndActiveServicesWithCurrentPricing() = runTest {
        val historicalApt = Appointment(
            id = "apt_1",
            name = "Kavita",
            phone = "9822001122",
            serviceSnapshot = "[{\"id\":\"s1\",\"name\":\"Keratin Spa\",\"price\":800}," +
                "{\"id\":\"s2\",\"name\":\"Threading\",\"price\":100}]",
            serviceIds = listOf("s1", "s2"),
            rate = 900,
            paid = 900,
            staff = "Sunita",
            date = "2026-10-15",
            time = "11:00",
            note = "",
            status = "done",
            reminded = null,
            bookedAt = 1000L
        )

        val fakeAppointmentRepo = FakeAppointmentRepo(listOf(historicalApt))
        val useCase = BuildRebookDraftUseCase(fakeAppointmentRepo, fakeServiceRepo)

        val result = useCase("9822001122")
        assertTrue(result is DataResult.Success)

        val draft = (result as DataResult.Success).data
        assertEquals("Kavita", draft.name)
        assertEquals("9822001122", draft.phone)
        assertEquals(2, draft.selectedServices.size)
        // Current catalog rate: 1000 + 150 = 1150
        assertEquals(1150, draft.subtotal)
        assertEquals(1000, draft.selectedServices.first { it.id == "s1" }.rate)
    }

    @Test
    fun invoke_withOrphanedDeletedService_filtersOutDeletedServiceCleanly() = runTest {
        val historicalApt = Appointment(
            id = "apt_2",
            name = "Kavita",
            phone = "9822001122",
            serviceSnapshot = "Keratin Spa + Promotional Diwali Pack",
            serviceIds = listOf("s1", "s_deleted_promo"),
            rate = 1400,
            paid = 1400,
            staff = "Sunita",
            date = "2026-10-20",
            time = "14:00",
            note = "",
            status = "done",
            reminded = null,
            bookedAt = 2000L
        )

        val fakeAppointmentRepo = FakeAppointmentRepo(listOf(historicalApt))
        val useCase = BuildRebookDraftUseCase(fakeAppointmentRepo, fakeServiceRepo)

        val result = useCase("+91 9822001122")
        assertTrue(result is DataResult.Success)

        val draft = (result as DataResult.Success).data
        assertEquals(1, draft.selectedServices.size)
        assertEquals("s1", draft.selectedServices[0].id)
        assertEquals(1000, draft.subtotal)
    }

    @Test
    fun invoke_withOnlyCancelledBookings_preFillsNameAndPhoneWithEmptyServices() = runTest {
        val cancelledApt = Appointment(
            id = "apt_3",
            name = "Kavita",
            phone = "9822001122",
            serviceSnapshot = "Hydra Facial",
            serviceIds = listOf("s3"),
            rate = 1500,
            paid = 0,
            staff = "Sunita",
            date = "2026-10-10",
            time = "10:00",
            note = "",
            status = "cancel",
            reminded = null,
            bookedAt = 500L
        )

        val fakeAppointmentRepo = FakeAppointmentRepo(listOf(cancelledApt))
        val useCase = BuildRebookDraftUseCase(fakeAppointmentRepo, fakeServiceRepo)

        val result = useCase("9822001122")
        assertTrue(result is DataResult.Success)

        val draft = (result as DataResult.Success).data
        assertEquals("Kavita", draft.name)
        assertEquals("9822001122", draft.phone)
        assertTrue(draft.selectedServices.isEmpty())
        assertEquals(0, draft.subtotal)
    }

    @Test
    fun invoke_withNoPriorAppointments_returnsEmptyDraftWithPhone() = runTest {
        val fakeAppointmentRepo = FakeAppointmentRepo(emptyList())
        val useCase = BuildRebookDraftUseCase(fakeAppointmentRepo, fakeServiceRepo)

        val result = useCase("9822001122")
        assertTrue(result is DataResult.Success)

        val draft = (result as DataResult.Success).data
        assertEquals("", draft.name)
        assertEquals("9822001122", draft.phone)
        assertTrue(draft.selectedServices.isEmpty())
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
        override suspend fun insertAppointment(
            appointment: Appointment
        ): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun updateAppointment(
            appointment: Appointment
        ): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun updateStatus(
            id: String,
            status: AppointmentStatus
        ): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun deleteAppointment(
            appointment: Appointment
        ): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun deleteById(id: String): DataResult<Unit> = DataResult.Success(Unit)
        override suspend fun countUpcomingAppointmentsForService(serviceId: String, today: String): Int = 0
        override suspend fun count(): Int = list.size
    }
}
