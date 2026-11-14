package com.uniquemakeover.admin.core.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PinLockManagerTest {

    private lateinit var context: Context
    private lateinit var pinHasher: PinHasher
    private lateinit var pinLockManager: PinLockManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        pinHasher = PinHasher()
        pinLockManager = PinLockManager(context, pinHasher)
    }

    @Test
    fun verifyPin_defaultPin1234_passesVerification() {
        val isValid = pinLockManager.verifyPin(DEFAULT_PIN)
        assertThat(isValid).isTrue()
        assertThat(pinLockManager.isDefaultPin()).isTrue()
    }

    @Test
    fun verifyPin_wrongPin_failsVerification() {
        val isValid = pinLockManager.verifyPin("0000")
        assertThat(isValid).isFalse()
    }

    @Test
    fun setPin_updatesPinSuccessfully() {
        pinLockManager.setPin("5678")

        assertThat(pinLockManager.verifyPin("5678")).isTrue()
        assertThat(pinLockManager.verifyPin("1234")).isFalse()
        assertThat(pinLockManager.isDefaultPin()).isFalse()
    }
}
