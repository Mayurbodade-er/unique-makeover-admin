package com.uniquemakeover.admin.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PhoneSanitizerTest {

    private lateinit var sanitizer: PhoneSanitizer

    @Before
    fun setUp() {
        sanitizer = PhoneSanitizer()
    }

    @Test
    fun sanitize_validNumbersWithFormatting_normalizesToTenDigits() {
        assertEquals("9822001122", sanitizer.sanitize("+91 98220-01122"))
        assertEquals("9822001122", sanitizer.sanitize("09822001122"))
        assertEquals("9822001122", sanitizer.sanitize("9822001122"))
        assertEquals("7888123456", sanitizer.sanitize("+91 78881 23456"))
        assertEquals("6123456789", sanitizer.sanitize("6123456789"))
    }

    @Test
    fun sanitize_invalidNumbers_returnsNull() {
        assertNull(sanitizer.sanitize("12345"))
        assertNull(sanitizer.sanitize("abcdefghij"))
        assertNull(sanitizer.sanitize("0000000000"))
        assertNull(sanitizer.sanitize("1234567890"))
        assertNull(sanitizer.sanitize("5555555555"))
        assertNull(sanitizer.sanitize(""))
    }

    @Test
    fun isValid_returnsTrueOnlyForValidNumbers() {
        assertTrue(sanitizer.isValid("9822001122"))
        assertTrue(sanitizer.isValid("+91 88888 99999"))
        assertFalse(sanitizer.isValid("12345"))
        assertFalse(sanitizer.isValid("1234567890"))
    }
}
