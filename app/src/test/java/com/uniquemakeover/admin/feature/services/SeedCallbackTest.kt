package com.uniquemakeover.admin.feature.services

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.uniquemakeover.admin.core.database.SeedCallback
import com.uniquemakeover.admin.core.database.UniqueMakeoverDatabase
import com.uniquemakeover.admin.core.database.entity.ServiceEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SeedCallbackTest {

    private lateinit var context: Context
    private lateinit var database: UniqueMakeoverDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            UniqueMakeoverDatabase::class.java
        )
            .allowMainThreadQueries()
            .addCallback(SeedCallback())
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun seedCallback_insertsSevenCuratedServicesOnDatabaseCreate() = runBlocking {
        // Trigger database creation by querying serviceDao
        val serviceDao = database.serviceDao()
        val services = serviceDao.getAllServicesSync()

        assertThat(services).hasSize(7)

        val namesAndRates = services.map { it.name to it.rate }
        assertThat(namesAndRates).containsExactly(
            "Haircut" to 150,
            "Threading" to 50,
            "Gold Facial" to 800,
            "Keratin Spa" to 1500,
            "Manicure" to 400,
            "Pedicure" to 500,
            "Bridal Package" to 5000
        ).inOrder()

        val sortOrders = services.map { it.sortOrder }
        assertThat(sortOrders).containsExactly(1, 2, 3, 4, 5, 6, 7).inOrder()
    }

    @Test
    fun seedCallback_customPricePersistsWithoutBeingOverwritten() = runBlocking {
        val serviceDao = database.serviceDao()
        val originalGoldFacial = serviceDao.getServiceByIdSync("s3")
        assertThat(originalGoldFacial?.rate).isEqualTo(800)

        // Owner updates Gold Facial to ₹1,000 (AC-SVC-01.2)
        serviceDao.update(
            ServiceEntity(
                id = "s3",
                name = "Gold Facial",
                rate = 1000,
                imagePath = null,
                sortOrder = 3
            )
        )

        val updated = serviceDao.getServiceByIdSync("s3")
        assertThat(updated?.rate).isEqualTo(1000)
    }
}
