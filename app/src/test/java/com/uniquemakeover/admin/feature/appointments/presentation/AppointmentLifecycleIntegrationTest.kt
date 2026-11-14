package com.uniquemakeover.admin.feature.appointments.presentation

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.uniquemakeover.admin.core.database.UniqueMakeoverDatabase
import com.uniquemakeover.admin.core.database.dao.AppointmentDao
import com.uniquemakeover.admin.core.database.entity.AppointmentEntity
import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.core.domain.model.StatusUpdateResult
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.data.repository.AppointmentRepositoryImpl
import com.uniquemakeover.admin.feature.appointments.data.repository.SlotRepositoryImpl
import com.uniquemakeover.admin.feature.appointments.domain.usecase.CancelAppointmentUseCase
import com.uniquemakeover.admin.feature.appointments.domain.usecase.MarkAppointmentDoneUseCase
import com.uniquemakeover.admin.feature.appointments.domain.usecase.UndoAppointmentStatusUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppointmentLifecycleIntegrationTest {

    private lateinit var database: UniqueMakeoverDatabase
    private lateinit var appointmentDao: AppointmentDao
    private lateinit var appointmentRepository: AppointmentRepositoryImpl
    private lateinit var slotRepository: SlotRepositoryImpl
    private lateinit var markDoneUseCase: MarkAppointmentDoneUseCase
    private lateinit var cancelUseCase: CancelAppointmentUseCase
    private lateinit var undoUseCase: UndoAppointmentStatusUseCase

    private val seedAppointment = AppointmentEntity(
        id = "apt_life_1",
        name = "Meera Deshmukh",
        phone = "9822334455",
        svc = "Hydra Facial",
        serviceIds = listOf("s1"),
        rate = 2000,
        paid = 500,
        staff = "Sunita",
        date = "2026-11-20",
        time = "14:00",
        note = "",
        status = AppointmentStatus.BOOKED.value,
        bookedAt = 1000L
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, UniqueMakeoverDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        appointmentDao = database.appointmentDao()
        appointmentRepository = AppointmentRepositoryImpl(appointmentDao, Dispatchers.Unconfined)
        slotRepository = SlotRepositoryImpl(appointmentDao, Dispatchers.Unconfined)

        markDoneUseCase = MarkAppointmentDoneUseCase(appointmentRepository)
        cancelUseCase = CancelAppointmentUseCase(appointmentRepository)
        undoUseCase = UndoAppointmentStatusUseCase(appointmentRepository, slotRepository)

        runBlocking {
            appointmentDao.insert(seedAppointment)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun markDone_andUndo_transitionsDatabaseStateCorrectly() = runBlocking {
        // Step 1: Mark Done
        val markDoneResult = markDoneUseCase("apt_life_1")
        assertTrue(markDoneResult is DataResult.Success)

        val doneEntity = appointmentDao.getAppointmentByIdSync("apt_life_1")
        assertEquals(AppointmentStatus.DONE.value, doneEntity?.status)

        // Step 2: Undo to Booked
        val undoResult = undoUseCase("apt_life_1", AppointmentStatus.BOOKED)
        assertTrue(undoResult is StatusUpdateResult.Success)

        val revertedEntity = appointmentDao.getAppointmentByIdSync("apt_life_1")
        assertEquals(AppointmentStatus.BOOKED.value, revertedEntity?.status)
    }

    @Test
    fun cancel_andUndo_whenSlotRebooked_preventsRevival() = runBlocking {
        // Step 1: Cancel appointment
        val cancelResult = cancelUseCase("apt_life_1")
        assertTrue(cancelResult is DataResult.Success)

        val cancelledEntity = appointmentDao.getAppointmentByIdSync("apt_life_1")
        assertEquals(AppointmentStatus.CANCEL.value, cancelledEntity?.status)

        // Step 2: Another client books that exact slot (2026-11-20, 14:00)
        val conflictingAppointment = AppointmentEntity(
            id = "apt_life_2",
            name = "Rani Kadam",
            phone = "9811223344",
            svc = "Cleanup",
            serviceIds = listOf("s2"),
            rate = 1000,
            paid = 1000,
            staff = "Sunita",
            date = "2026-11-20",
            time = "14:00",
            note = "",
            status = AppointmentStatus.BOOKED.value,
            bookedAt = 2000L
        )
        appointmentDao.insert(conflictingAppointment)

        // Step 3: Attempt Undo on apt_life_1
        val undoResult = undoUseCase("apt_life_1", AppointmentStatus.BOOKED)
        assertTrue(undoResult is StatusUpdateResult.SlotConflict)

        // Step 4: Ensure apt_life_1 remains cancelled in database
        val finalEntity = appointmentDao.getAppointmentByIdSync("apt_life_1")
        assertEquals(AppointmentStatus.CANCEL.value, finalEntity?.status)
    }
}
