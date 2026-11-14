package com.uniquemakeover.admin.feature.appointments.presentation.booking

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.domain.model.Slot
import com.uniquemakeover.admin.feature.appointments.domain.model.BookingDraft

data class BookingUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val availableServices: List<Service> = emptyList(),
    val staffList: List<String> = emptyList(),
    val availableSlots: List<Slot> = emptyList(),
    val draft: BookingDraft = BookingDraft(),
    val nameError: String? = null,
    val phoneError: String? = null,
    val serviceError: String? = null,
    val slotError: String? = null,
    val financialError: String? = null,
    val generalError: String? = null,
    val bookingSuccess: Boolean = false
)
