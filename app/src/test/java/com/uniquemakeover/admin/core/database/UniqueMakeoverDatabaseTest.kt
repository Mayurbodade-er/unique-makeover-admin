package com.uniquemakeover.admin.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.uniquemakeover.admin.core.database.dao.AppointmentDao
import com.uniquemakeover.admin.core.database.dao.ClientNoteDao
import com.uniquemakeover.admin.core.database.dao.PaymentDao
import com.uniquemakeover.admin.core.database.dao.ServiceDao
import com.uniquemakeover.admin.core.database.dao.SettingsDao
import com.uniquemakeover.admin.core.database.entity.AppointmentEntity
import com.uniquemakeover.admin.core.database.entity.ClientNoteEntity
import com.uniquemakeover.admin.core.database.entity.PaymentEntity
import com.uniquemakeover.admin.core.database.entity.ServiceEntity
import com.uniquemakeover.admin.core.database.entity.SettingsEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UniqueMakeoverDatabaseTest {

    private lateinit var database: UniqueMakeoverDatabase
    private lateinit var settingsDao: SettingsDao
    private lateinit var serviceDao: ServiceDao
    private lateinit var appointmentDao: AppointmentDao
    private lateinit var clientNoteDao: ClientNoteDao
    private lateinit var paymentDao: PaymentDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            UniqueMakeoverDatabase::class.java
        )
            .allowMainThreadQueries()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL("PRAGMA foreign_keys = ON;")
                }
            })
            .build()

        settingsDao = database.settingsDao()
        serviceDao = database.serviceDao()
        appointmentDao = database.appointmentDao()
        clientNoteDao = database.clientNoteDao()
        paymentDao = database.paymentDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun database_indicesCreatedInSqliteMaster() {
        val cursor = database.openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type = 'index'"
        )
        val indexNames = mutableListOf<String>()
        while (cursor.moveToNext()) {
            indexNames.add(cursor.getString(0))
        }
        cursor.close()

        assertThat(indexNames).contains("index_appointments_date")
        assertThat(indexNames).contains("index_appointments_phone")
        assertThat(indexNames).contains("index_appointments_date_time")
        assertThat(indexNames).contains("index_payments_appointmentId")
    }

    @Test
    fun settingsDao_insertAndRetrieveSettings() = runBlocking {
        val settings = SettingsEntity(
            id = 1,
            shop = "Unique Makeover",
            tagline = "Luxury Beauty Parlour",
            adminPinHash = "dummy_hash",
            wa = "919876543210",
            upi = "unique@upi",
            staff = "Sunita,Pooja",
            open = "09:00",
            close = "20:00",
            slot = 30,
            msg = "Reminder template"
        )

        settingsDao.insert(settings)

        val retrieved = settingsDao.getSettings().first()
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.shop).isEqualTo("Unique Makeover")
        assertThat(retrieved?.adminPinHash).isEqualTo("dummy_hash")
    }

    @Test
    fun serviceDao_insertAndQueryOrderedBySortOrderAndName() = runBlocking {
        val svc1 = ServiceEntity(id = "s1", name = "Z-Facial", rate = 800, sortOrder = 2)
        val svc2 = ServiceEntity(id = "s2", name = "A-Haircut", rate = 150, sortOrder = 1)
        val svc3 = ServiceEntity(id = "s3", name = "B-Threading", rate = 50, sortOrder = 1)

        serviceDao.insertAll(listOf(svc1, svc2, svc3))

        val services = serviceDao.getAllServicesSync()
        assertThat(services).hasSize(3)
        // Sort order 1 before sort order 2; within 1, "A-Haircut" before "B-Threading"
        assertThat(services[0].id).isEqualTo("s2")
        assertThat(services[1].id).isEqualTo("s3")
        assertThat(services[2].id).isEqualTo("s1")
    }

    @Test
    fun appointmentDao_clashDetectionRespectsTimeStatusAndExcludeId() = runBlocking {
        val appt1 = AppointmentEntity(
            id = "appt_1",
            name = "Rani Patil",
            phone = "9876543210",
            svc = "Gold Facial",
            serviceIds = listOf("s1"),
            rate = 800,
            paid = 0,
            staff = "Sunita",
            date = "2026-11-15",
            time = "10:00",
            note = "First visit",
            status = "CONFIRMED",
            bookedAt = System.currentTimeMillis()
        )
        val cancelledAppt = AppointmentEntity(
            id = "appt_2",
            name = "Anjali Deshmukh",
            phone = "9876543211",
            svc = "Haircut",
            serviceIds = listOf("s2"),
            rate = 150,
            paid = 0,
            staff = "Pooja",
            date = "2026-11-15",
            time = "10:30",
            note = "",
            status = "CANCELLED",
            bookedAt = System.currentTimeMillis()
        )

        appointmentDao.insert(appt1)
        appointmentDao.insert(cancelledAppt)

        // 1. Exact match at 10:00 -> 1 clash
        val clash1 = appointmentDao.countClashingAppointments("2026-11-15", "10:00")
        assertThat(clash1).isEqualTo(1)

        // 2. Different time -> 0 clash
        val clashDifferentTime = appointmentDao.countClashingAppointments("2026-11-15", "11:00")
        assertThat(clashDifferentTime).isEqualTo(0)

        // 3. CANCELLED appointment slot at 10:30 is freed -> 0 clash
        val clashCancelled = appointmentDao.countClashingAppointments("2026-11-15", "10:30")
        assertThat(clashCancelled).isEqualTo(0)

        // 4. Editing appt1 itself (excludeId = "appt_1") -> 0 clash
        val clashSelfExclude = appointmentDao.countClashingAppointments(
            date = "2026-11-15",
            time = "10:00",
            excludeId = "appt_1"
        )
        assertThat(clashSelfExclude).isEqualTo(0)
    }

    @Test
    fun clientNoteDao_upsertUpdatesExistingNote() = runBlocking {
        val phone = "9876543210"
        val note1 = ClientNoteEntity(phone = phone, note = "Sensitive skin", updatedAt = 1000L)
        clientNoteDao.upsert(note1)

        val retrieved1 = clientNoteDao.getNoteByPhoneSync(phone)
        assertThat(retrieved1?.note).isEqualTo("Sensitive skin")

        val note2 = ClientNoteEntity(phone = phone, note = "Sensitive skin. Prefers herbal wax.", updatedAt = 2000L)
        clientNoteDao.upsert(note2)

        val retrieved2 = clientNoteDao.getNoteByPhoneSync(phone)
        assertThat(retrieved2?.note).isEqualTo("Sensitive skin. Prefers herbal wax.")
    }

    @Test
    fun paymentDao_insertAndCalculateTotalPaid() = runBlocking {
        val apptId = "appt_100"
        val payment1 = PaymentEntity(id = "p1", appointmentId = apptId, amount = 500, mode = "CASH", paidAt = 1000L)
        val payment2 = PaymentEntity(id = "p2", appointmentId = apptId, amount = 300, mode = "UPI", paidAt = 2000L)

        paymentDao.insertAll(listOf(payment1, payment2))

        val payments = paymentDao.getPaymentsForAppointmentSync(apptId)
        assertThat(payments).hasSize(2)

        val total = paymentDao.getTotalPaidForAppointment(apptId)
        assertThat(total).isEqualTo(800)
    }

    @Test
    fun database_concurrentStressReadAndWriteLoop() = runBlocking {
        // Run concurrent inserts across 8 coroutines
        val jobs = (1..8).map { threadId ->
            async {
                for (i in 1..25) {
                    val entity = AppointmentEntity(
                        id = "thread_${threadId}_appt_$i",
                        name = "Client $i",
                        phone = "98000000$threadId",
                        svc = "Service $i",
                        serviceIds = listOf("s1"),
                        rate = 200,
                        paid = 0,
                        staff = "Staff $threadId",
                        date = "2026-11-15",
                        time = "10:$i",
                        note = "Stress test",
                        status = "CONFIRMED",
                        bookedAt = System.currentTimeMillis()
                    )
                    appointmentDao.insert(entity)
                    val count = appointmentDao.count()
                    assertThat(count).isAtLeast(1)
                }
            }
        }

        jobs.awaitAll()

        val totalCount = appointmentDao.count()
        assertThat(totalCount).isEqualTo(200)
    }
}
