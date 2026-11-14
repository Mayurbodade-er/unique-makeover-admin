package com.uniquemakeover.admin.feature.settings.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.uniquemakeover.admin.core.database.UniqueMakeoverDatabase
import com.uniquemakeover.admin.core.database.dao.SettingsDao
import com.uniquemakeover.admin.core.database.entity.SettingsEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsDaoTest {

    private lateinit var database: UniqueMakeoverDatabase
    private lateinit var settingsDao: SettingsDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            UniqueMakeoverDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        settingsDao = database.settingsDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun settingsDao_updateOperationsPreservesPinHashAndBranding() = runBlocking {
        val initialSettings = SettingsEntity(
            id = 1,
            shop = "Unique Makeover",
            tagline = "Luxury Beauty Parlour",
            adminPinHash = "secure_pin_hash_1234",
            logoPath = "images/logo.png",
            bgPath = null,
            wa = "919876543210",
            upi = "unique@upi",
            staff = "Sunita",
            open = "10:00",
            close = "20:00",
            slot = 30,
            msg = "Reminder template"
        )

        settingsDao.insert(initialSettings)

        val current = settingsDao.getSettingsSync()
        assertThat(current).isNotNull()

        val updatedOperations = current!!.copy(
            open = "09:30",
            close = "20:30",
            slot = 45,
            staff = "Sunita,Pooja,Priya"
        )
        settingsDao.upsert(updatedOperations)

        val retrieved = settingsDao.getSettingsSync()
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.open).isEqualTo("09:30")
        assertThat(retrieved?.close).isEqualTo("20:30")
        assertThat(retrieved?.slot).isEqualTo(45)
        assertThat(retrieved?.staff).isEqualTo("Sunita,Pooja,Priya")

        // Invariant: PIN and branding were NOT overwritten
        assertThat(retrieved?.adminPinHash).isEqualTo("secure_pin_hash_1234")
        assertThat(retrieved?.shop).isEqualTo("Unique Makeover")
        assertThat(retrieved?.logoPath).isEqualTo("images/logo.png")
    }
}
