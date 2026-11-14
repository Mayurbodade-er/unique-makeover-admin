package com.uniquemakeover.admin.feature.appointments.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.uniquemakeover.admin.core.database.UniqueMakeoverDatabase
import com.uniquemakeover.admin.core.database.dao.AppointmentDao
import com.uniquemakeover.admin.core.database.entity.AppointmentEntity
import com.uniquemakeover.admin.feature.appointments.data.repository.SlotRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppointmentEditClashIntegrationTest {

    private lateinit var database: UniqueMakeoverDatabase
    private lateinit var appointmentDao: AppointmentDao
    private lateinit var slotRepository: SlotRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            UniqueMakeoverDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        appointmentDao = database.appointmentDao()
        slotRepository = SlotRepositoryImpl(appointmentDao, Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun selfExclusionAllowsSameSlotWhilePreventingClashesWithOtherBookings() = runBlocking {
        val targetDate = "2026-11-25"

        val aptA = AppointmentEntity(
            id = "apt_101",
            name = "Pooja Patil",
            phone = "9822001122",
            svc = "Gold Facial",
            serviceIds = listOf("svc_01"),
            rate = 800,
            paid = 0,
            staff = "Sunita",
            date = targetDate,
            time = "10:00",
            note = "",
            status = "booked",
            bookedAt = System.currentTimeMillis()
        )
        val aptB = AppointmentEntity(
            id = "apt_102",
            name = "Rani Deshmukh",
            phone = "9822003344",
            svc = "Keratin Spa",
            serviceIds = listOf("svc_02"),
            rate = 1500,
            paid = 500,
            staff = "Sunita",
            date = targetDate,
            time = "11:00",
            note = "",
            status = "booked",
            bookedAt = System.currentTimeMillis()
        )

        appointmentDao.insert(aptA)
        appointmentDao.insert(aptB)

        // AC-APT-04.1: Self-exclusion from clash check
        val selfClashCount = appointmentDao.countClashingAppointmentsExcludingId(
            date = targetDate,
            time = "10:00",
            excludeId = aptA.id
        )
        assertEquals(0, selfClashCount)
        assertTrue(slotRepository.isClashFree(targetDate, "10:00", excludeId = aptA.id))

        // AC-APT-04.2: Clash prevention against other bookings
        val otherClashCount = appointmentDao.countClashingAppointmentsExcludingId(
            date = targetDate,
            time = "11:00",
            excludeId = aptA.id
        )
        assertEquals(1, otherClashCount)
        assertFalse(slotRepository.isClashFree(targetDate, "11:00", excludeId = aptA.id))
    }

    @Test
    fun cancelledAppointmentsDoNotTriggerClashEvenIfDifferentId() = runBlocking {
        val targetDate = "2026-11-25"
        val aptA = AppointmentEntity(
            id = "apt_101",
            name = "Pooja Patil",
            phone = "9822001122",
            svc = "Gold Facial",
            serviceIds = listOf("svc_01"),
            rate = 800,
            paid = 0,
            staff = "Sunita",
            date = targetDate,
            time = "10:00",
            note = "",
            status = "booked",
            bookedAt = System.currentTimeMillis()
        )
        val aptCancelled = AppointmentEntity(
            id = "apt_cancelled",
            name = "Kavita Rao",
            phone = "9822005566",
            svc = "Cleanup",
            serviceIds = listOf("svc_03"),
            rate = 400,
            paid = 0,
            staff = "Sunita",
            date = targetDate,
            time = "14:00",
            note = "",
            status = "cancel",
            bookedAt = System.currentTimeMillis()
        )
        appointmentDao.insert(aptA)
        appointmentDao.insert(aptCancelled)

        val clashCount = appointmentDao.countClashingAppointmentsExcludingId(
            date = targetDate,
            time = "14:00",
            excludeId = aptA.id
        )
        assertEquals(0, clashCount)
        assertTrue(slotRepository.isClashFree(targetDate, "14:00", excludeId = aptA.id))
    }
}
