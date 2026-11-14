package com.uniquemakeover.admin.feature.auth.domain.model

sealed interface PinResult {
    data object Success : PinResult
    data object Invalid : PinResult
    data class Locked(val remainingSeconds: Int) : PinResult
}
