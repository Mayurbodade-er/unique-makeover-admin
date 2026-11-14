package com.uniquemakeover.admin.feature.appointments.domain.usecase

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.domain.model.Slot
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.PhoneSanitizer
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.model.BookingDraft
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.appointments.domain.repository.SlotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EditAppointmentUseCaseTest {

    private val service1 = Service("svc_01", "Gold Facial", 800)
    private val service2 = Service("svc_02", "Hair Spa", 700)

    private val baseAppointment = Appointment(
        id = "apt_101",
        name = "Pooja Patil",
        phone = "9822001122",
        serviceSnapshot = "Gold Facial",
        serviceIds = listOf("svc_01"),
        rate = 800,
        paid = 0,
        staff = "Sunita",
        date = "2026-11-25",
        time = "11:00",
        note = "",
        status = "booked",
        bookedAt = 1700000000000L
    )

    private lateinit var appointmentRepository: FakeAppointmentRepository
    private lateinit var slotRepository: FakeSlotRepository
    private lateinit var validateBookingDraftUseCase: ValidateBookingDraftUseCase
    private lateinit var phoneSanitizer: PhoneSanitizer
    private lateinit var editAppointmentUseCase: EditAppointmentUseCase

    @Before
    fun setUp() {
        appointmentRepository = FakeAppointmentRepository()
        slotRepository = FakeSlotRepository()
        phoneSanitizer = PhoneSanitizer()
        validateBookingDraftUseCase = ValidateBookingDraftUseCase(phoneSanitizer)
        editAppointmentUseCase = EditAppointmentUseCase(
            appointmentRepository = appointmentRepository,
            slotRepository = slotRepository,
            validateBookingDraftUseCase = validateBookingDraftUseCase,
            phoneSanitizer = phoneSanitizer
        )
    }

    @Test
    fun selfExclusionAllowsSameSlot() = runBlocking {
        appointmentRepository.insertAppointment(baseAppointment)
        slotRepository.occupiedSlots.add("2026-11-25_11:00") // Occupied by apt_101

        val draft = BookingDraft(
            name = "Pooja Patil",
            phone = "9822001122",
            selectedServices = listOf(service1),
            date = "2026-11-25",
            time = "11:00",
            staff = "Sunita"
        )

        val result = editAppointmentUseCase("apt_101", draft)
        assertTrue(result is DataResult.Success)

        val updated = appointmentRepository.getAppointmentById("apt_101")
        assertEquals("11:00", updated?.time)
    }

    @Test
    fun clashRejectionWhenTargetSlotBelongsToAnotherBooking() = runBlocking {
        appointmentRepository.insertAppointment(baseAppointment)

        // apt_102 occupies 16:00
        slotRepository.occupiedSlots.add("2026-11-25_16:00")

        val draft = BookingDraft(
            name = "Pooja Patil",
            phone = "9822001122",
            selectedServices = listOf(service1),
            date = "2026-11-25",
            time = "16:00",
            staff = "Sunita"
        )

        val result = editAppointmentUseCase("apt_101", draft)
        assertTrue(result is DataResult.Error)
        val error = result as DataResult.Error
        val errorMsg = (error.message as UiText.DynamicString).value
        assertTrue(errorMsg.contains("Slot was just booked by another client"))
    }

    @Test
    fun financialRecalculationOnServiceChangePreservesPaidAndTimestamps() = runBlocking {
        val existing = baseAppointment.copy(rate = 800, paid = 500)
        appointmentRepository.insertAppointment(existing)

        val draft = BookingDraft(
            name = "Pooja Patil",
            phone = "9822001122",
            selectedServices = listOf(service1, service2), // 800 + 700 = 1500
            discount = 100, // 1500 - 100 = 1400
            date = "2026-11-25",
            time = "11:00",
            staff = "Sunita"
        )

        val result = editAppointmentUseCase("apt_101", draft)
        assertTrue(result is DataResult.Success)

        val updated = appointmentRepository.getAppointmentById("apt_101")
        assertEquals(1400, updated?.rate)
        assertEquals(500, updated?.paid)
        assertEquals("apt_101", updated?.id)
        assertEquals(1700000000000L, updated?.bookedAt)
        assertEquals("Gold Facial + Hair Spa", updated?.serviceSnapshot)
    }

    @Test
    fun completedAppointmentRejectsDateOrTimeChange() = runBlocking {
        val existing = baseAppointment.copy(status = "done")
        appointmentRepository.insertAppointment(existing)

        val draft = BookingDraft(
            name = "Pooja Patil",
            phone = "9822001122",
            selectedServices = listOf(service1),
            date = "2026-11-26",
            time = "11:00",
            staff = "Sunita"
        )

        val result = editAppointmentUseCase("apt_101", draft)
        assertTrue(result is DataResult.Error)
        val error = result as DataResult.Error
        val msg = (error.message as UiText.DynamicString).value
        assertTrue(msg.contains("To reschedule, first restore appointment status to Booked"))
    }

    @Test
    fun completedAppointmentAllowsStaffOrNoteChangeWithoutRescheduling() = runBlocking {
        val existing = baseAppointment.copy(status = "done", staff = "Sunita")
        appointmentRepository.insertAppointment(existing)

        val draft = BookingDraft(
            name = "Pooja Patil",
            phone = "9822001122",
            selectedServices = listOf(service1),
            date = "2026-11-25",
            time = "11:00",
            staff = "Pooja",
            internalNote = "Preferred stylist requested"
        )

        val result = editAppointmentUseCase("apt_101", draft)
        assertTrue(result is DataResult.Success)

        val updated = appointmentRepository.getAppointmentById("apt_101")
        assertEquals("Pooja", updated?.staff)
        assertEquals("Preferred stylist requested", updated?.note)
    }

    @Test
    fun invalidDraftReturnsValidationError() = runBlocking {
        appointmentRepository.insertAppointment(baseAppointment)

        val invalidDraft = BookingDraft(
            name = "",
            phone = "123",
            selectedServices = emptyList()
        )

        val result = editAppointmentUseCase("apt_101", invalidDraft)
        assertTrue(result is DataResult.Error)
    }

    @Test
    fun appointmentNotFoundReturnsError() = runBlocking {
        val draft = BookingDraft(
            name = "Pooja Patil",
            phone = "9822001122",
            selectedServices = listOf(service1),
            date = "2026-11-25",
            time = "11:00"
        )
        val result = editAppointmentUseCase("non_existent", draft)
        assertTrue(result is DataResult.Error)
    }

    private class FakeAppointmentRepository : AppointmentRepository {
        private val appointments = mutableMapOf<String, Appointment>()

        override fun getAllAppointments(): Flow<List<Appointment>> = emptyFlow()
        override fun getAppointmentsByDate(date: String): Flow<List<Appointment>> = emptyFlow()
        override suspend fun getAppointmentsByDateSync(date: String): List<Appointment> = emptyList()
        override suspend fun getAppointmentById(id: String): Appointment? = appointments[id]
        override fun getTodaySchedule(today: String): Flow<List<Appointment>> = emptyFlow()
        override fun getFilteredAppointments(
            status: String?,
            startDate: String?,
            endDate: String?,
            query: String
        ): Flow<List<Appointment>> = emptyFlow()

        override suspend fun insertAppointment(appointment: Appointment): DataResult<Unit> {
            appointments[appointment.id] = appointment
            return DataResult.Success(Unit)
        }

        override suspend fun updateAppointment(appointment: Appointment): DataResult<Unit> {
            appointments[appointment.id] = appointment
            return DataResult.Success(Unit)
        }

        override suspend fun updateStatus(id: String, status: AppointmentStatus): DataResult<Unit> =
            DataResult.Success(Unit)

        override suspend fun deleteAppointment(appointment: Appointment): DataResult<Unit> {
            appointments.remove(appointment.id)
            return DataResult.Success(Unit)
        }

        override suspend fun deleteById(id: String): DataResult<Unit> {
            appointments.remove(id)
            return DataResult.Success(Unit)
        }

        override suspend fun count(): Int = appointments.size
    }

    private class FakeSlotRepository : SlotRepository {
        val occupiedSlots = mutableSetOf<String>()

        override suspend fun isClashFree(date: String, time: String, excludeId: String?): Boolean {
            val key = "${date}_$time"
            return if (excludeId != null && occupiedSlots.contains(key) && key.endsWith("11:00")) {
                true // Self-exclusion simulated
            } else {
                !occupiedSlots.contains(key)
            }
        }

        override suspend fun getAvailableSlots(date: String): List<Slot> = emptyList()
    }
}
