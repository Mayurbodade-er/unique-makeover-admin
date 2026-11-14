package com.uniquemakeover.admin.core.security

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class PinHasherTest {

    private lateinit var pinHasher: PinHasher

    @Before
    fun setUp() {
        pinHasher = PinHasher()
    }

    @Test
    fun hashPin_generatesDistinctHashesForSamePinDueToSalt() {
        val hash1 = pinHasher.hashPin("1234")
        val hash2 = pinHasher.hashPin("1234")

        assertThat(hash1).isNotEqualTo(hash2)
        assertThat(hash1).contains(":")
        assertThat(hash2).contains(":")
    }

    @Test
    fun verifyPin_returnsTrueForMatchingPin() {
        val hash = pinHasher.hashPin("4321")
        val isMatch = pinHasher.verifyPin("4321", hash)

        assertThat(isMatch).isTrue()
    }

    @Test
    fun verifyPin_returnsFalseForIncorrectPin() {
        val hash = pinHasher.hashPin("4321")
        val isMatch = pinHasher.verifyPin("9999", hash)

        assertThat(isMatch).isFalse()
    }

    @Test
    fun verifyPin_returnsFalseForMalformedHash() {
        assertThat(pinHasher.verifyPin("1234", "malformed_hash")).isFalse()
    }
}
