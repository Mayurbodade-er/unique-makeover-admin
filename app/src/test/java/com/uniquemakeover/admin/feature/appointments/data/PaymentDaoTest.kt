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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentDaoTest {

    private lateinit var database: UniqueMakeoverDatabase
    private lateinit var paymentDao: PaymentDao
    private lateinit var appointmentDao: AppointmentDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, UniqueMakeoverDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        paymentDao = database.paymentDao()
        appointmentDao = database.appointmentDao()

        // Seed parent appointment for foreign key / relational integrity
        runBlocking {
            appointmentDao.insert(
                AppointmentEntity(
                    id = "apt_test_1",
                    name = "Kavita",
                    phone = "9822001122",
                    svc = "Facial",
                    serviceIds = listOf("s1"),
                    rate = 1000,
                    paid = 0,
                    staff = "Sunita",
                    date = "2026-11-20",
                    time = "10:00",
                    note = "",
                    status = "booked",
                    bookedAt = 1000L
                )
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQueryPayments_returnsPaymentsInOrder() = runBlocking {
        val p1 = PaymentEntity("p1", "apt_test_1", 300, "cash", 1000L)
        val p2 = PaymentEntity("p2", "apt_test_1", 700, "upi", 2000L)

        paymentDao.insertAll(listOf(p1, p2))

        val payments = paymentDao.getPaymentsForAppointmentSync("apt_test_1")
        assertEquals(2, payments.size)
        assertEquals("p1", payments[0].id)
        assertEquals(300, payments[0].amount)
        assertEquals("p2", payments[1].id)
        assertEquals("upi", payments[1].mode)

        val total = paymentDao.getTotalPaidForAppointment("apt_test_1")
        assertEquals(1000, total)
    }
}
