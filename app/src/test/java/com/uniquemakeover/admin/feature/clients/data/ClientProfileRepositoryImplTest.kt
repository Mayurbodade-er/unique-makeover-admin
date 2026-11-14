package com.uniquemakeover.admin.feature.clients.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.uniquemakeover.admin.core.database.UniqueMakeoverDatabase
import com.uniquemakeover.admin.core.database.dao.AppointmentDao
import com.uniquemakeover.admin.core.database.entity.AppointmentEntity
import com.uniquemakeover.admin.core.util.PhoneSanitizer
import com.uniquemakeover.admin.feature.clients.data.repository.ClientProfileRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ClientProfileRepositoryImplTest {

    private lateinit var database: UniqueMakeoverDatabase
    private lateinit var appointmentDao: AppointmentDao
    private lateinit var repository: ClientProfileRepositoryImpl
    private val phoneSanitizer = PhoneSanitizer()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, UniqueMakeoverDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        appointmentDao = database.appointmentDao()
        repository = ClientProfileRepositoryImpl(
            appointmentDao = appointmentDao,
            phoneSanitizer = phoneSanitizer,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun acCrm01_1_aggregatesVisitsAndLifetimeSpend_correctly() = runTest(testDispatcher) {
        val apt1 = createAppointment(
            TestApt("apt_1", "Neha", "9822001122", 500, "done", "2026-11-01", 1000L)
        )
        val apt2 = createAppointment(
            TestApt("apt_2", "Neha", "9822001122", 1200, "done", "2026-11-10", 2000L)
        )
        val apt3 = createAppointment(
            TestApt("apt_3", "Neha", "9822001122", 800, "booked", "2026-11-20", 3000L)
        )
        appointmentDao.insertAll(listOf(apt1, apt2, apt3))

        val profiles = repository.getClientProfiles().first()

        assertEquals(1, profiles.size)
        val profile = profiles.first()
        assertEquals("9822001122", profile.phone)
        assertEquals("Neha", profile.name)
        assertEquals(3, profile.totalVisits)
        assertEquals(2500, profile.totalSpend)
        assertEquals("2026-11-20", profile.lastVisitDate)
    }

    @Test
    fun acCrm01_2_cancelledBookingsExcludedFromSpendAndVisits() = runTest(testDispatcher) {
        val completedApt = createAppointment(
            TestApt("apt_c1", "Priya", "9876543210", 1000, "done", "2026-11-05", 1000L)
        )
        val cancelledApt = createAppointment(
            TestApt("apt_c2", "Priya", "9876543210", 2000, "cancel", "2026-11-12", 2000L)
        )
        appointmentDao.insertAll(listOf(completedApt, cancelledApt))

        val profiles = repository.getClientProfiles().first()

        assertEquals(1, profiles.size)
        val profile = profiles.first()
        assertEquals(1, profile.totalVisits)
        assertEquals(1000, profile.totalSpend)
    }

    @Test
    fun acCrm01_3_deduplicatesBySanitizedPhoneVariants() = runTest(testDispatcher) {
        val apt1 = createAppointment(
            TestApt("apt_d1", "Sneha", "9822001122", 600, "done", "2026-11-01", 1000L)
        )
        val apt2 = createAppointment(
            TestApt("apt_d2", "Sneha Patil", "+91 98220 01122", 400, "done", "2026-11-15", 2000L)
        )
        appointmentDao.insertAll(listOf(apt1, apt2))

        val profiles = repository.getClientProfiles().first()

        assertEquals(1, profiles.size)
        val profile = profiles.first()
        assertEquals("9822001122", profile.phone)
        assertEquals("Sneha Patil", profile.name)
        assertEquals(2, profile.totalVisits)
        assertEquals(1000, profile.totalSpend)
    }

    @Test
    fun ecCrm01_1_selectsLatestNameVariationAcrossVisits() = runTest(testDispatcher) {
        val visit1 = createAppointment(
            TestApt("apt_v1", "Neha", "9822001122", 500, "done", "2026-10-01", 1000L)
        )
        val visit2 = createAppointment(
            TestApt("apt_v2", "Neha Kulkarni", "9822001122", 700, "done", "2026-11-14", 5000L)
        )
        appointmentDao.insertAll(listOf(visit1, visit2))

        val profiles = repository.getClientProfiles().first()

        assertEquals(1, profiles.size)
        assertEquals("Neha Kulkarni", profiles[0].name)
    }

    @Test
    fun ecCrm01_3_clientWithOnlyCancelledBookings_showsZeroVisitsAndZeroSpend() = runTest(testDispatcher) {
        val cancel1 = createAppointment(
            TestApt("apt_x1", "Anita", "9988776655", 800, "cancel", "2026-11-01", 1000L)
        )
        val cancel2 = createAppointment(
            TestApt("apt_x2", "Anita", "9988776655", 1200, "cancel", "2026-11-05", 2000L)
        )
        appointmentDao.insertAll(listOf(cancel1, cancel2))

        val profiles = repository.getClientProfiles().first()

        assertEquals(1, profiles.size)
        val profile = profiles[0]
        assertEquals("Anita", profile.name)
        assertEquals(0, profile.totalVisits)
        assertEquals(0, profile.totalSpend)
    }

    @Test
    fun acCrm02_1_searchByPartialName_returnsMatchingProfile() = runTest(testDispatcher) {
        val apt1 = createAppointment(
            TestApt("apt_s1", "Radhika Apte", "9822123456", 2000, "done", "2026-11-10", 1000L)
        )
        val apt2 = createAppointment(
            TestApt("apt_s2", "Anjali Sharma", "9876543210", 800, "done", "2026-11-11", 2000L)
        )
        appointmentDao.insertAll(listOf(apt1, apt2))

        val results = repository.searchClientProfiles("radh").first()

        assertEquals(1, results.size)
        assertEquals("Radhika Apte", results[0].name)
    }

    @Test
    fun acCrm02_2_searchByPartialPhoneSubstring_returnsMatchingProfile() = runTest(testDispatcher) {
        val apt1 = createAppointment(
            TestApt("apt_p1", "Sneha", "9822123456", 1500, "done", "2026-11-01", 1000L)
        )
        val apt2 = createAppointment(
            TestApt("apt_p2", "Meera", "9876500000", 500, "done", "2026-11-02", 2000L)
        )
        appointmentDao.insertAll(listOf(apt1, apt2))

        val results1 = repository.searchClientProfiles("1234").first()
        val results2 = repository.searchClientProfiles("3456").first()

        assertEquals(1, results1.size)
        assertEquals("Sneha", results1[0].name)
        assertEquals(1, results2.size)
        assertEquals("Sneha", results2[0].name)
    }

    @Test
    fun ecCrm02_2_searchWithRegionalMarathiDevanagari_matchesSafely() = runTest(testDispatcher) {
        val apt1 = createAppointment(
            TestApt("apt_u1", "पूजा पाटील", "9822998877", 1200, "done", "2026-11-05", 1000L)
        )
        val apt2 = createAppointment(
            TestApt("apt_u2", "Rani", "9822112233", 800, "done", "2026-11-06", 2000L)
        )
        appointmentDao.insertAll(listOf(apt1, apt2))

        val results = repository.searchClientProfiles("पूजा").first()

        assertEquals(1, results.size)
        assertEquals("पूजा पाटील", results[0].name)
    }

    @Test
    fun ecCrm02_3_searchWithWhitespacePadding_trimsAndMatches() = runTest(testDispatcher) {
        val apt = createAppointment(
            TestApt("apt_w1", "Kavita", "9822123456", 900, "done", "2026-11-08", 1000L)
        )
        appointmentDao.insertAll(listOf(apt))

        val results = repository.searchClientProfiles("  9822123456  ").first()

        assertEquals(1, results.size)
        assertEquals("Kavita", results[0].name)
    }

    private data class TestApt(
        val id: String,
        val name: String,
        val phone: String,
        val rate: Int,
        val status: String,
        val date: String,
        val bookedAt: Long
    )

    private fun createAppointment(apt: TestApt): AppointmentEntity {
        return AppointmentEntity(
            id = apt.id,
            name = apt.name,
            phone = apt.phone,
            svc = "Bridal Makeover",
            serviceIds = listOf("s1"),
            rate = apt.rate,
            paid = apt.rate,
            staff = "Sunita",
            date = apt.date,
            time = "11:00",
            note = "",
            status = apt.status,
            reminded = null,
            bookedAt = apt.bookedAt
        )
    }
}
