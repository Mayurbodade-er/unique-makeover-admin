package com.uniquemakeover.admin.feature.services.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.uniquemakeover.admin.core.database.UniqueMakeoverDatabase
import com.uniquemakeover.admin.core.database.dao.AppointmentDao
import com.uniquemakeover.admin.core.database.dao.ServiceDao
import com.uniquemakeover.admin.core.database.entity.AppointmentEntity
import com.uniquemakeover.admin.core.database.entity.ServiceEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SnapshotSafetyIntegrationTest {

    private lateinit var database: UniqueMakeoverDatabase
    private lateinit var serviceDao: ServiceDao
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

        serviceDao = database.serviceDao()
        appointmentDao = database.appointmentDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun updatingAndDeletingCatalogService_preservesAppointmentHistoricalSnapshot() = runBlocking {
        val serviceEntity = ServiceEntity(
            id = "s_gold",
            name = "Gold Facial",
            rate = 800,
            imagePath = null,
            sortOrder = 1
        )
        serviceDao.insert(serviceEntity)

        val appointment = AppointmentEntity(
            id = "apt_hist_01",
            name = "Pooja Patil",
            phone = "9822001122",
            svc = "Gold Facial",
            serviceIds = listOf("s_gold"),
            rate = 800,
            paid = 800,
            staff = "Sunita",
            date = "2026-11-20",
            time = "10:00",
            note = "Bridal glow",
            status = "done",
            reminded = null,
            bookedAt = 1000L
        )
        appointmentDao.insert(appointment)

        // 1. Update Service in catalog to ₹1,200 with changed title
        serviceDao.update(
            serviceEntity.copy(name = "Gold Facial Luxury", rate = 1200)
        )

        // Verify catalog updated
        val updatedService = serviceDao.getServiceByIdSync("s_gold")
        assertNotNull(updatedService)
        assertEquals("Gold Facial Luxury", updatedService?.name)
        assertEquals(1200, updatedService?.rate)

        // Verify historical appointment strictly retains original rate and title snapshot
        val appointmentAfterPriceChange = appointmentDao.getAppointmentByIdSync("apt_hist_01")
        assertNotNull(appointmentAfterPriceChange)
        assertEquals(800, appointmentAfterPriceChange?.rate)
        assertEquals("Gold Facial", appointmentAfterPriceChange?.svc)

        // 2. Delete Service from catalog
        serviceDao.deleteById("s_gold")
        val deletedService = serviceDao.getServiceByIdSync("s_gold")
        assertNull(deletedService)

        // Verify historical appointment still exists intact with all details
        val appointmentAfterServiceDelete = appointmentDao.getAppointmentByIdSync("apt_hist_01")
        assertNotNull(appointmentAfterServiceDelete)
        assertEquals("apt_hist_01", appointmentAfterServiceDelete?.id)
        assertEquals(800, appointmentAfterServiceDelete?.rate)
        assertEquals("Gold Facial", appointmentAfterServiceDelete?.svc)
        assertEquals(listOf("s_gold"), appointmentAfterServiceDelete?.serviceIds)
    }
}
