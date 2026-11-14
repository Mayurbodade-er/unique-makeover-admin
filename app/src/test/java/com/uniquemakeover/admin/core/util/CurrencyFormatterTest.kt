package com.uniquemakeover.admin.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun zeroAmount_formatsToZeroRupees() {
        assertEquals("₹ 0", CurrencyFormatter.formatInr(0))
    }

    @Test
    fun smallAmountsUnderThousand_formatsWithoutCommas() {
        assertEquals("₹ 500", CurrencyFormatter.formatInr(500))
        assertEquals("₹ 999", CurrencyFormatter.formatInr(999))
    }

    @Test
    fun thousands_formatsWithSingleComma() {
        assertEquals("₹ 1,000", CurrencyFormatter.formatInr(1000))
        assertEquals("₹ 3,500", CurrencyFormatter.formatInr(3500))
        assertEquals("₹ 10,000", CurrencyFormatter.formatInr(10000))
    }

    @Test
    fun lakhs_formatsWithIndianTwoDigitGrouping() {
        assertEquals("₹ 1,50,000", CurrencyFormatter.formatInr(150000))
        assertEquals("₹ 12,34,567", CurrencyFormatter.formatInr(1234567))
    }

    @Test
    fun crores_formatsProperly() {
        assertEquals("₹ 1,00,00,000", CurrencyFormatter.formatInr(10000000))
    }

    @Test
    fun negativeAmount_formatsWithMinusPrefix() {
        assertEquals("-₹ 500", CurrencyFormatter.formatInr(-500))
        assertEquals("-₹ 1,000", CurrencyFormatter.formatInr(-1000))
    }
}
