package com.uniquemakeover.admin.feature.auth.domain.model

sealed interface LockoutState {
    data object Unlocked : LockoutState
    data class Locked(val remainingSeconds: Int) : LockoutState
}
