package com.uniquemakeover.admin.feature.appointments.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.uniquemakeover.admin.core.database.UniqueMakeoverDatabase
import com.uniquemakeover.admin.core.database.dao.AppointmentDao
import com.uniquemakeover.admin.core.database.entity.AppointmentEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.system.measureTimeMillis

private const val SEED_RECORDS_COUNT = 100
private const val SUBSTRING_SEARCH_TIME_LIMIT_MS = 50L

@RunWith(RobolectricTestRunner::class)
class AppointmentDaoFilterTest {

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
    fun observeFilteredAppointments_filtersByStatusAndDateRange() = runBlocking {
        seedSampleAppointments()

        val upcoming = appointmentDao.observeFilteredAppointments(
            status = "booked",
            startDate = "2026-11-20",
            endDate = null,
            query = ""
        ).first()
        assertEquals(1, upcoming.size)
        assertEquals("apt_1", upcoming[0].id)

        val completed = appointmentDao.observeFilteredAppointments(
            status = "done",
            startDate = null,
            endDate = null,
            query = ""
        ).first()
        assertEquals(1, completed.size)
        assertEquals("apt_2", completed[0].id)

        val cancelled = appointmentDao.observeFilteredAppointments(
            status = "cancel",
            startDate = null,
            endDate = null,
            query = ""
        ).first()
        assertEquals(1, cancelled.size)
        assertEquals("apt_3", cancelled[0].id)
    }

    @Test
    fun observeFilteredAppointments_filtersBySubstringQuery() = runBlocking {
        seedSampleAppointments()

        val nameSearch = appointmentDao.observeFilteredAppointments(
            status = null,
            startDate = null,
            endDate = null,
            query = "sharma"
        ).first()
        assertEquals(1, nameSearch.size)
        assertEquals("apt_1", nameSearch[0].id)

        val phoneSearch = appointmentDao.observeFilteredAppointments(
            status = null,
            startDate = null,
            endDate = null,
            query = "1234"
        ).first()
        assertEquals(1, phoneSearch.size)
        assertEquals("apt_3", phoneSearch[0].id)
    }

    @Test
    fun observeFilteredAppointments_performanceSubFiftyMillisAcrossHundredRecords() = runBlocking {
        val records = (1..SEED_RECORDS_COUNT).map { index ->
            val status = when (index % 3) {
                0 -> "done"
                1 -> "booked"
                else -> "cancelled"
            }
            val paddedDay = (1 + (index % 28)).toString().padStart(2, '0')
            AppointmentEntity(
                id = "bulk_$index",
                name = "Client $index Sharma",
                phone = "98000000$index",
                svc = "Service $index",
                serviceIds = listOf("s1"),
                rate = 1000,
                paid = 500,
                staff = "Sunita",
                date = "2026-11-$paddedDay",
                time = "10:00",
                note = "",
                status = status,
                bookedAt = System.currentTimeMillis()
            )
        }

        appointmentDao.insertAll(records)

        val duration = measureTimeMillis {
            val results = appointmentDao.observeFilteredAppointments(
                status = "booked",
                startDate = "2026-11-10",
                endDate = "2026-11-20",
                query = "Sharma"
            ).first()
            assertTrue(results.isNotEmpty())
        }

        val message = "Query execution took $duration ms (limit: ${SUBSTRING_SEARCH_TIME_LIMIT_MS}ms)"
        assertTrue(message, duration < SUBSTRING_SEARCH_TIME_LIMIT_MS)
    }

    private suspend fun seedSampleAppointments() {
        val apt1 = AppointmentEntity(
            id = "apt_1",
            name = "Sunita Sharma",
            phone = "9876543210",
            svc = "Gold Facial",
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
        val apt2 = AppointmentEntity(
            id = "apt_2",
            name = "Pooja Patil",
            phone = "9822112233",
            svc = "Hair Spa",
            serviceIds = listOf("s2"),
            rate = 1500,
            paid = 1500,
            staff = "Pooja",
            date = "2026-11-21",
            time = "11:00",
            note = "",
            status = "done",
            bookedAt = 2000L
        )
        val apt3 = AppointmentEntity(
            id = "apt_3",
            name = "Rani Deshmukh",
            phone = "9850123456",
            svc = "Manicure",
            serviceIds = listOf("s3"),
            rate = 500,
            paid = 0,
            staff = "Sunita",
            date = "2026-11-19",
            time = "12:00",
            note = "",
            status = "cancelled",
            bookedAt = 3000L
        )
        appointmentDao.insertAll(listOf(apt1, apt2, apt3))
    }
}
