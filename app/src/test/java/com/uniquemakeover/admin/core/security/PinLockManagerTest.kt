package com.uniquemakeover.admin.core.security

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.uniquemakeover.admin.feature.auth.domain.model.LockoutState
import com.uniquemakeover.admin.feature.auth.domain.model.PinResult
import com.uniquemakeover.admin.testutil.FakeTimeProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class PinLockManagerTest {

    private lateinit var context: Context
    private lateinit var pinHasher: PinHasher
    private lateinit var lockoutPolicy: LockoutPolicy
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var pinLockManager: PinLockManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        pinHasher = PinHasher()
        lockoutPolicy = LockoutPolicy()
        fakeTimeProvider = FakeTimeProvider(currentElapsedRealtime = 10000L)

        val uniqueName = "test_auth_prefs_${UUID.randomUUID()}"
        val testDataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(uniqueName) }
        )

        pinLockManager = PinLockManager(
            context = context,
            pinHasher = pinHasher,
            lockoutPolicy = lockoutPolicy,
            timeProvider = fakeTimeProvider,
            dataStore = testDataStore
        )
    }

    @Test
    fun verifyPin_defaultPin1234_passesVerification() = runTest {
        val result = pinLockManager.verifyPin(DEFAULT_PIN)
        assertThat(result).isEqualTo(PinResult.Success)
        assertThat(pinLockManager.isDefaultPin()).isTrue()
    }

    @Test
    fun verifyPin_wrongPin_failsVerification() = runTest {
        val result = pinLockManager.verifyPin("0000")
        assertThat(result).isEqualTo(PinResult.Invalid)
    }

    @Test
    fun setPin_updatesPinSuccessfully() = runTest {
        pinLockManager.setPin("5678")

        assertThat(pinLockManager.verifyPin("5678")).isEqualTo(PinResult.Success)
        assertThat(pinLockManager.verifyPin("1234")).isEqualTo(PinResult.Invalid)
        assertThat(pinLockManager.isDefaultPin()).isFalse()
    }

    @Test
    fun verifyPin_fiveConsecutiveFailures_triggersLockout() = runTest {
        repeat(4) {
            val res = pinLockManager.verifyPin("0000")
            assertThat(res).isEqualTo(PinResult.Invalid)
        }

        // 5th failed attempt triggers Lockout
        val fifthRes = pinLockManager.verifyPin("0000")
        assertThat(fifthRes).isInstanceOf(PinResult.Locked::class.java)
        assertThat((fifthRes as PinResult.Locked).remainingSeconds).isEqualTo(30)

        // Further attempt while locked is rejected
        val lockedAttempt = pinLockManager.verifyPin(DEFAULT_PIN)
        assertThat(lockedAttempt).isInstanceOf(PinResult.Locked::class.java)
    }

    @Test
    fun verifyPin_correctPinResetsFailureCounter() = runTest {
        // 3 failures
        repeat(3) {
            pinLockManager.verifyPin("0000")
        }

        // Correct PIN
        val success = pinLockManager.verifyPin(DEFAULT_PIN)
        assertThat(success).isEqualTo(PinResult.Success)

        // Next failure is only attempt 1 (not locked)
        val afterReset = pinLockManager.verifyPin("0000")
        assertThat(afterReset).isEqualTo(PinResult.Invalid)
    }

    @Test
    fun observeLockoutState_initiallyUnlocked() = runTest {
        val state = pinLockManager.observeLockoutState().first()
        assertThat(state).isEqualTo(LockoutState.Unlocked)
    }
}
