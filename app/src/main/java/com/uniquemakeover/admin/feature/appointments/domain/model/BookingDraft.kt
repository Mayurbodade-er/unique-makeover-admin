package com.uniquemakeover.admin.feature.appointments.domain.model

import com.uniquemakeover.admin.core.domain.model.Service

data class BookingDraft(
    val name: String = "",
    val phone: String = "",
    val selectedServices: List<Service> = emptyList(),
    val date: String = "",
    val time: String = "",
    val staff: String = "",
    val discount: Int = 0,
    val advancePaid: Int = 0,
    val internalNote: String = ""
) {
    val subtotal: Int get() = selectedServices.sumOf { it.rate }
    val finalRate: Int get() = (subtotal - discount).coerceAtLeast(0)
    val balanceDue: Int get() = (finalRate - advancePaid).coerceAtLeast(0)
}
