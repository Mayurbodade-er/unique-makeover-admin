package com.uniquemakeover.admin.feature.auth.domain.repository

import com.uniquemakeover.admin.feature.auth.domain.model.LockoutState
import com.uniquemakeover.admin.feature.auth.domain.model.PinResult
import kotlinx.coroutines.flow.Flow

interface PinRepository {
    suspend fun verifyPin(enteredPin: String): PinResult
    suspend fun isDefaultPin(): Boolean
    suspend fun changePin(newPin: String)
    fun observeLockoutState(): Flow<LockoutState>
}
