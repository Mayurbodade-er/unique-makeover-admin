package com.uniquemakeover.admin.feature.auth.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.uniquemakeover.admin.core.database.UniqueMakeoverDatabase
import com.uniquemakeover.admin.core.database.dao.SettingsDao
import com.uniquemakeover.admin.core.database.entity.SettingsEntity
import com.uniquemakeover.admin.core.security.LockoutPolicy
import com.uniquemakeover.admin.core.security.PinHasher
import com.uniquemakeover.admin.core.security.PinLockManager
import com.uniquemakeover.admin.feature.auth.domain.model.ChangePinResult
import com.uniquemakeover.admin.feature.auth.domain.model.PinResult
import com.uniquemakeover.admin.feature.auth.domain.usecase.ChangePinUseCase
import com.uniquemakeover.admin.testutil.FakeTimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ChangePinIntegrationTest {

    private lateinit var context: Context
    private lateinit var database: UniqueMakeoverDatabase
    private lateinit var settingsDao: SettingsDao
    private lateinit var pinHasher: PinHasher
    private lateinit var pinLockManager: PinLockManager
    private lateinit var pinRepository: PinRepositoryImpl
    private lateinit var changePinUseCase: ChangePinUseCase

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, UniqueMakeoverDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settingsDao = database.settingsDao()

        pinHasher = PinHasher()
        val defaultHash = pinHasher.hashPin("1234")
        val initialSettings = SettingsEntity(
            id = 1,
            shop = "Unique Makeover",
            tagline = "Beauty Parlour",
            adminPinHash = defaultHash,
            wa = "9876543210",
            upi = "unique@upi",
            staff = "Sunita",
            open = "09:00",
            close = "20:00",
            slot = 30,
            msg = "Reminder"
        )
        settingsDao.insert(initialSettings)

        val testDataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("test_pin_${UUID.randomUUID()}") }
        )
        pinLockManager = PinLockManager(
            context = context,
            pinHasher = pinHasher,
            lockoutPolicy = LockoutPolicy(),
            timeProvider = FakeTimeProvider(),
            dataStore = testDataStore
        )

        val testDispatcher = UnconfinedTestDispatcher()
        pinRepository = PinRepositoryImpl(pinLockManager, settingsDao, testDispatcher)
        changePinUseCase = ChangePinUseCase(pinRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun changePin_updatesKeystoreAndSynchronizesRoomDatabase() = runTest {
        // Initial state: 1234 works, 8899 fails
        assertThat(pinLockManager.verifyPin("1234")).isEqualTo(PinResult.Success)
        assertThat(pinLockManager.verifyPin("8899")).isEqualTo(PinResult.Invalid)

        // Change PIN to 8899
        val result = changePinUseCase(
            currentPin = "1234",
            newPin = "8899",
            confirmPin = "8899"
        )
        assertThat(result).isEqualTo(ChangePinResult.Success)

        // Subsequent verifications: 1234 fails, 8899 passes
        assertThat(pinLockManager.verifyPin("1234")).isEqualTo(PinResult.Invalid)
        assertThat(pinLockManager.verifyPin("8899")).isEqualTo(PinResult.Success)
        assertThat(pinLockManager.isDefaultPin()).isFalse()

        // Database settings entity is synchronized with the new hash
        val updatedSettings = settingsDao.getSettingsSync()
        assertThat(updatedSettings).isNotNull()
        val isNewHashMatching = pinHasher.verifyPin("8899", updatedSettings!!.adminPinHash)
        assertThat(isNewHashMatching).isTrue()
    }
}
