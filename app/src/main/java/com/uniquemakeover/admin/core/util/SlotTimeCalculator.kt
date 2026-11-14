package com.uniquemakeover.admin.core.util

private const val MINUTES_PER_HOUR = 60
private const val NOON_HOUR = 12
private const val TIME_PARTS_COUNT = 2

object SlotTimeCalculator {

    fun timeToMinutes(time24: String): Int {
        val parts = time24.split(":")
        if (parts.size != TIME_PARTS_COUNT) return 0
        val hour = parts[0].toIntOrNull() ?: 0
        val minute = parts[1].toIntOrNull() ?: 0
        return hour * MINUTES_PER_HOUR + minute
    }

    fun minutesToTime(minutes: Int): String {
        val hour = minutes / MINUTES_PER_HOUR
        val minute = minutes % MINUTES_PER_HOUR
        return "%02d:%02d".format(hour, minute)
    }

    fun format12Hour(time24: String): String {
        val parts = time24.split(":")
        if (parts.size != TIME_PARTS_COUNT) return time24
        val hour24 = parts[0].toIntOrNull() ?: 0
        val minute = parts[1].toIntOrNull() ?: 0

        val amPm = if (hour24 >= NOON_HOUR) "PM" else "AM"
        val hour12 = when {
            hour24 == 0 -> NOON_HOUR
            hour24 > NOON_HOUR -> hour24 - NOON_HOUR
            else -> hour24
        }
        return "%02d:%02d %s".format(hour12, minute, amPm)
    }

    fun format24Hour(hour12: Int, minute: Int, isPm: Boolean): String {
        val hour24 = when {
            isPm && hour12 < NOON_HOUR -> hour12 + NOON_HOUR
            !isPm && hour12 == NOON_HOUR -> 0
            else -> hour12
        }
        return "%02d:%02d".format(hour24, minute)
    }
}
