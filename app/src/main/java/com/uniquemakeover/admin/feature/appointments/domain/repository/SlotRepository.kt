package com.uniquemakeover.admin.feature.appointments.domain.repository

import com.uniquemakeover.admin.core.domain.model.Slot

interface SlotRepository {
    suspend fun isClashFree(date: String, time: String, excludeId: String? = null): Boolean
    suspend fun getAvailableSlots(date: String): List<Slot>
}
