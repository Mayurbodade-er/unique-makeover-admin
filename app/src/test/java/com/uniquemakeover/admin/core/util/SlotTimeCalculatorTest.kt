package com.uniquemakeover.admin.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SlotTimeCalculatorTest {

    @Test
    fun timeToMinutes_convertsCorrectly() {
        assertThat(SlotTimeCalculator.timeToMinutes("00:00")).isEqualTo(0)
        assertThat(SlotTimeCalculator.timeToMinutes("10:00")).isEqualTo(600)
        assertThat(SlotTimeCalculator.timeToMinutes("14:30")).isEqualTo(870)
        assertThat(SlotTimeCalculator.timeToMinutes("23:59")).isEqualTo(1439)
        assertThat(SlotTimeCalculator.timeToMinutes("invalid")).isEqualTo(0)
    }

    @Test
    fun minutesToTime_convertsCorrectly() {
        assertThat(SlotTimeCalculator.minutesToTime(0)).isEqualTo("00:00")
        assertThat(SlotTimeCalculator.minutesToTime(600)).isEqualTo("10:00")
        assertThat(SlotTimeCalculator.minutesToTime(870)).isEqualTo("14:30")
    }

    @Test
    fun format12Hour_formatsAmAndPmCorrectly() {
        assertThat(SlotTimeCalculator.format12Hour("10:00")).isEqualTo("10:00 AM")
        assertThat(SlotTimeCalculator.format12Hour("14:30")).isEqualTo("02:30 PM")
        assertThat(SlotTimeCalculator.format12Hour("00:15")).isEqualTo("12:15 AM")
        assertThat(SlotTimeCalculator.format12Hour("12:00")).isEqualTo("12:00 PM")
        assertThat(SlotTimeCalculator.format12Hour("20:45")).isEqualTo("08:45 PM")
    }

    @Test
    fun format24Hour_converts12HourClockTo24Hour() {
        assertThat(SlotTimeCalculator.format24Hour(10, 0, false)).isEqualTo("10:00")
        assertThat(SlotTimeCalculator.format24Hour(2, 30, true)).isEqualTo("14:30")
        assertThat(SlotTimeCalculator.format24Hour(12, 15, false)).isEqualTo("00:15")
        assertThat(SlotTimeCalculator.format24Hour(12, 0, true)).isEqualTo("12:00")
    }
}
