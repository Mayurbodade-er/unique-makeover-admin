package com.uniquemakeover.admin.feature.appointments.presentation.booking

import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.domain.model.Slot

data class BookingActions(
    val onNameChange: (String) -> Unit = {},
    val onPhoneChange: (String) -> Unit = {},
    val onToggleService: (Service) -> Unit = {},
    val onSelectSlot: (Slot) -> Unit = {},
    val onSelectStaff: (String) -> Unit = {},
    val onDiscountChange: (Int) -> Unit = {},
    val onAdvanceChange: (Int) -> Unit = {},
    val onSave: () -> Unit = {}
)
