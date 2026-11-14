package com.uniquemakeover.admin.feature.auth.presentation

import com.uniquemakeover.admin.core.common.UiText

data class PinLockUiState(
    val digitsEntered: Int = 0,
    val isError: Boolean = false,
    val errorMessage: UiText? = null,
    val shakeTrigger: Long = 0L,
    val isAuthenticated: Boolean = false
)
