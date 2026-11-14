package com.uniquemakeover.admin.feature.appointments.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.uniquemakeover.admin.core.database.UniqueMakeoverDatabase
import com.uniquemakeover.admin.core.database.dao.AppointmentDao
import com.uniquemakeover.admin.core.database.dao.PaymentDao
import com.uniquemakeover.admin.core.database.entity.AppointmentEntity
import com.uniquemakeover.admin.core.database.entity.PaymentEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppointmentCascadeDeleteTest {

    private lateinit var database: UniqueMakeoverDatabase
    private lateinit var appointmentDao: AppointmentDao
    private lateinit var paymentDao: PaymentDao

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
        paymentDao = database.paymentDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun hardDeleteAppointment_purgesAppointmentAndCascadesPaymentRecords() = runBlocking {
        val appointmentId = "apt_test_01"
        val appointment = AppointmentEntity(
            id = appointmentId,
            name = "Rani Deshmukh",
            phone = "9822003344",
            svc = "Keratin Spa",
            serviceIds = listOf("svc_02"),
            rate = 1500,
            paid = 1000,
            staff = "Sunita",
            date = "2026-11-20",
            time = "14:00",
            note = "",
            status = "booked",
            bookedAt = System.currentTimeMillis()
        )
        val payment1 = PaymentEntity(
            id = "pay_01",
            appointmentId = appointmentId,
            amount = 500,
            mode = "cash",
            paidAt = System.currentTimeMillis()
        )
        val payment2 = PaymentEntity(
            id = "pay_02",
            appointmentId = appointmentId,
            amount = 500,
            mode = "upi",
            paidAt = System.currentTimeMillis()
        )

        appointmentDao.insert(appointment)
        paymentDao.insert(payment1)
        paymentDao.insert(payment2)

        // Verify initial insertion
        assertEquals(1, appointmentDao.count())
        assertEquals(2, paymentDao.getPaymentsForAppointmentSync(appointmentId).size)

        // Execute cascade delete
        paymentDao.deletePaymentsByAppointmentId(appointmentId)
        appointmentDao.deleteById(appointmentId)

        // Assert appointment is gone
        assertNull(appointmentDao.getAppointmentByIdSync(appointmentId))
        assertEquals(0, appointmentDao.count())

        // Assert payment records are purged
        assertEquals(0, paymentDao.getPaymentsForAppointmentSync(appointmentId).size)
    }

    @Test
    fun deleteById_isIdempotent_whenDeletingNonExistentRecord() = runBlocking {
        // Should execute safely without throwing exceptions (EC-APT-06.1)
        val deletedRows = appointmentDao.deleteById("non_existent_id")
        assertEquals(0, deletedRows)
    }
}
