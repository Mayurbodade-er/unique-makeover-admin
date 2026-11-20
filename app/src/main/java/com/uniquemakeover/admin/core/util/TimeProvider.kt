package com.uniquemakeover.admin.core.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

interface TimeProvider {
    fun nowTime24(): String
    fun todayDate(): String
    fun tomorrowDate(): String {
        return runCatching {
            LocalDate.parse(todayDate()).plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        }.getOrDefault("")
    }
    fun currentTimeMillis(): Long
}

@Singleton
class DefaultTimeProvider @Inject constructor() : TimeProvider {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun nowTime24(): String = LocalTime.now().format(timeFormatter)

    override fun todayDate(): String = LocalDate.now().format(dateFormatter)

    override fun tomorrowDate(): String = LocalDate.now().plusDays(1).format(dateFormatter)

    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
