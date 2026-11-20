package com.uniquemakeover.admin.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpiValidatorTest {

    @Test
    fun isValid_validUpiHandles_returnsTrue() {
        assertTrue(UpiValidator.isValid("uniquemakeover@upi"))
        assertTrue(UpiValidator.isValid("sunita.salon@okaxis"))
        assertTrue(UpiValidator.isValid("9822001122@paytm"))
        assertTrue(UpiValidator.isValid("parlour_jalgaon@okhdfcbank"))
        assertTrue(UpiValidator.isValid("makeover-123@ybl"))
        assertTrue(UpiValidator.isValid("  uniquemakeover@upi  ")) // auto-trimmed
    }

    @Test
    fun isValid_invalidUpiHandles_returnsFalse() {
        assertFalse(UpiValidator.isValid(""))
        assertFalse(UpiValidator.isValid("   "))
        assertFalse(UpiValidator.isValid("uniquemakeover")) // missing @bank
        assertFalse(UpiValidator.isValid("@bank")) // missing username
        assertFalse(UpiValidator.isValid("uniquemakeover@")) // missing bank handle
        assertFalse(UpiValidator.isValid("u@bank")) // username too short (< 2)
        assertFalse(UpiValidator.isValid("uniquemakeover@b")) // handle too short (< 2)
        assertFalse(UpiValidator.isValid("unique makeover@upi")) // space inside username
        assertFalse(UpiValidator.isValid("unique@upi bank")) // space inside handle
    }
}
