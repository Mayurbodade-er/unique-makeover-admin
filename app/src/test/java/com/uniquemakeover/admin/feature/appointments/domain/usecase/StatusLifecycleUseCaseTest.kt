package com.uniquemakeover.admin.feature.appointments.domain.usecase

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.core.domain.model.Slot
import com.uniquemakeover.admin.core.domain.model.StatusUpdateResult
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.appointments.domain.repository.SlotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StatusLifecycleUseCaseTest {

    private lateinit var fakeRepository: FakeAppointmentRepository
    private lateinit var fakeSlotRepository: FakeSlotRepository
    private lateinit var markDoneUseCase: MarkAppointmentDoneUseCase
    private lateinit var cancelUseCase: CancelAppointmentUseCase
    private lateinit var undoUseCase: UndoAppointmentStatusUseCase

    private val sampleAppointment = Appointment(
        id = "apt_1",
        name = "Kavita Patil",
        phone = "9876543210",
        serviceSnapshot = "Hydra Facial",
        serviceIds = listOf("s1"),
        rate = 1500,
        paid = 500,
        staff = "Sunita",
        date = "2026-11-20",
        time = "11:00",
        note = "",
        status = AppointmentStatus.BOOKED.value,
        bookedAt = 1000L
    )

    @Before
    fun setUp() {
        fakeRepository = FakeAppointmentRepository(mutableMapOf(sampleAppointment.id to sampleAppointment))
        fakeSlotRepository = FakeSlotRepository(isClashFreeResponse = true)
        markDoneUseCase = MarkAppointmentDoneUseCase(fakeRepository)
        cancelUseCase = CancelAppointmentUseCase(fakeRepository)
        undoUseCase = UndoAppointmentStatusUseCase(fakeRepository, fakeSlotRepository)
    }

    @Test
    fun markAppointmentDone_fromBooked_updatesStatusToDone() = runBlocking {
        val result = markDoneUseCase("apt_1")
        assertTrue(result is DataResult.Success)
        assertEquals(AppointmentStatus.DONE.value, fakeRepository.getAppointmentById("apt_1")?.status)
    }

    @Test
    fun markAppointmentDone_fromCancelled_returnsErrorInvalidTransition() = runBlocking {
        fakeRepository.setAppointment(sampleAppointment.copy(status = AppointmentStatus.CANCEL.value))
        val result = markDoneUseCase("apt_1")
        assertTrue(result is DataResult.Error)
        val errorMsg = ((result as DataResult.Error).message as UiText.DynamicString).value
        assertEquals("Cannot mark cancelled appointment as Done directly", errorMsg)
    }

    @Test
    fun cancelAppointment_fromBooked_updatesStatusToCancel() = runBlocking {
        val result = cancelUseCase("apt_1")
        assertTrue(result is DataResult.Success)
        assertEquals(AppointmentStatus.CANCEL.value, fakeRepository.getAppointmentById("apt_1")?.status)
    }

    @Test
    fun cancelAppointment_fromDone_returnsErrorInvalidTransition() = runBlocking {
        fakeRepository.setAppointment(sampleAppointment.copy(status = AppointmentStatus.DONE.value))
        val result = cancelUseCase("apt_1")
        assertTrue(result is DataResult.Error)
        val errorMsg = ((result as DataResult.Error).message as UiText.DynamicString).value
        assertEquals("Cannot cancel a completed appointment directly", errorMsg)
    }

    @Test
    fun undoStatus_fromDoneToBooked_restoresStatus() = runBlocking {
        fakeRepository.setAppointment(sampleAppointment.copy(status = AppointmentStatus.DONE.value))
        val result = undoUseCase("apt_1", AppointmentStatus.BOOKED)
        assertTrue(result is StatusUpdateResult.Success)
        assertEquals(AppointmentStatus.BOOKED.value, fakeRepository.getAppointmentById("apt_1")?.status)
    }

    @Test
    fun undoStatus_fromCancelToBooked_whenSlotFree_restoresStatus() = runBlocking {
        fakeRepository.setAppointment(sampleAppointment.copy(status = AppointmentStatus.CANCEL.value))
        fakeSlotRepository.isClashFreeResponse = true

        val result = undoUseCase("apt_1", AppointmentStatus.BOOKED)
        assertTrue(result is StatusUpdateResult.Success)
        assertEquals(AppointmentStatus.BOOKED.value, fakeRepository.getAppointmentById("apt_1")?.status)
    }

    @Test
    fun undoStatus_fromCancelToBooked_whenSlotRebooked_returnsSlotConflict() = runBlocking {
        fakeRepository.setAppointment(sampleAppointment.copy(status = AppointmentStatus.CANCEL.value))
        fakeSlotRepository.isClashFreeResponse = false

        val result = undoUseCase("apt_1", AppointmentStatus.BOOKED)
        assertTrue(result is StatusUpdateResult.SlotConflict)
        assertEquals(
            "Slot is now occupied by another client. Please reschedule.",
            ((result as StatusUpdateResult.SlotConflict).message as UiText.DynamicString).value
        )
        // Ensure status remained CANCEL
        assertEquals(AppointmentStatus.CANCEL.value, fakeRepository.getAppointmentById("apt_1")?.status)
    }

    @Test
    fun undoStatus_whenAppointmentNotFound_returnsNotFound() = runBlocking {
        val result = undoUseCase("non_existent", AppointmentStatus.BOOKED)
        assertTrue(result is StatusUpdateResult.NotFound)
    }

    private class FakeAppointmentRepository(
        private val storage: MutableMap<String, Appointment>
    ) : AppointmentRepository {
        fun setAppointment(appointment: Appointment) {
            storage[appointment.id] = appointment
        }

        override fun getAllAppointments(): Flow<List<Appointment>> = flowOf(storage.values.toList())
        override fun getAppointmentsByDate(date: String): Flow<List<Appointment>> =
            flowOf(storage.values.filter { it.date == date })
        override suspend fun getAppointmentsByDateSync(date: String): List<Appointment> =
            storage.values.filter { it.date == date }
        override fun getTodaySchedule(today: String): Flow<List<Appointment>> =
            flowOf(storage.values.filter { it.date == today })
        override suspend fun getAppointmentById(id: String): Appointment? = storage[id]
        override fun getFilteredAppointments(
            status: String?,
            startDate: String?,
            endDate: String?,
            query: String
        ): Flow<List<Appointment>> = flowOf(storage.values.toList())

        override suspend fun insertAppointment(appointment: Appointment): DataResult<Unit> {
            storage[appointment.id] = appointment
            return DataResult.Success(Unit)
        }

        override suspend fun updateAppointment(appointment: Appointment): DataResult<Unit> {
            storage[appointment.id] = appointment
            return DataResult.Success(Unit)
        }

        override suspend fun updateStatus(id: String, status: AppointmentStatus): DataResult<Unit> {
            val existing = storage[id] ?: return DataResult.Error(UiText.DynamicString("Not found"))
            storage[id] = existing.copy(status = status.value)
            return DataResult.Success(Unit)
        }

        override suspend fun deleteAppointment(appointment: Appointment): DataResult<Unit> {
            storage.remove(appointment.id)
            return DataResult.Success(Unit)
        }

        override suspend fun count(): Int = storage.size
    }

    private class FakeSlotRepository(var isClashFreeResponse: Boolean) : SlotRepository {
        override suspend fun isClashFree(date: String, time: String, excludeId: String?): Boolean = isClashFreeResponse
        override suspend fun getAvailableSlots(date: String): List<Slot> = emptyList()
    }
}
