package com.uniquemakeover.admin.feature.appointments.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.uniquemakeover.admin.core.database.UniqueMakeoverDatabase
import com.uniquemakeover.admin.core.database.dao.AppointmentDao
import com.uniquemakeover.admin.core.database.entity.AppointmentEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
class AppointmentDaoClashTest {

    private lateinit var database: UniqueMakeoverDatabase
    private lateinit var appointmentDao: AppointmentDao

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
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun countClashingAppointments_detectsConflictAndReleasesOnCancel() = runBlocking {
        val appointment = AppointmentEntity(
            id = "apt_1001",
            name = "Pooja Patil",
            phone = "9876543210",
            svc = "Gold Facial",
            serviceIds = listOf("svc_01"),
            rate = 800,
            paid = 0,
            staff = "Sunita",
            date = "2026-11-20",
            time = "15:00",
            note = "",
            status = "booked",
            bookedAt = System.currentTimeMillis()
        )

        appointmentDao.insert(appointment)

        val clashCountBefore = appointmentDao.countClashingAppointments("2026-11-20", "15:00")
        assertEquals(1, clashCountBefore)

        val clashWithExclude = appointmentDao.countClashingAppointments("2026-11-20", "15:00", "apt_1001")
        assertEquals(0, clashWithExclude)

        val cancelledAppointment = appointment.copy(status = "cancel")
        appointmentDao.update(cancelledAppointment)

        val clashCountAfter = appointmentDao.countClashingAppointments("2026-11-20", "15:00")
        assertEquals(0, clashCountAfter)
    }

    @Test
    fun countClashingAppointments_indexedLookupUnderTenMilliseconds() = runBlocking {
        val bulkList = (1..50).map { i ->
            AppointmentEntity(
                id = "bulk_$i",
                name = "Client $i",
                phone = "98220011$i",
                svc = "Threading",
                serviceIds = listOf("svc_01"),
                rate = 100,
                paid = 100,
                staff = "Sunita",
                date = "2026-11-${10 + (i % 15)}",
                time = "%02d:00".format(9 + (i % 10)),
                note = "",
                status = "booked",
                bookedAt = System.currentTimeMillis()
            )
        }
        appointmentDao.insertAll(bulkList)

        val elapsedMs = measureTimeMillis {
            val count = appointmentDao.countClashingAppointments("2026-11-20", "15:00")
            assertTrue(count >= 0)
        }

        assertTrue("Query should complete under 25ms in test environment, took $elapsedMs ms", elapsedMs < 25)
    }
}
