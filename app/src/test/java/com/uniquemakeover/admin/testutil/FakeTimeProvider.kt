package com.uniquemakeover.admin.testutil

import com.uniquemakeover.admin.core.util.TimeProvider

class FakeTimeProvider(
    var currentTime24: String = "10:00",
    var currentDate: String = "2026-11-20",
    var currentMillis: Long = 1763632800000L
) : TimeProvider {
    override fun nowTime24(): String = currentTime24
    override fun todayDate(): String = currentDate
    override fun currentTimeMillis(): Long = currentMillis
}
