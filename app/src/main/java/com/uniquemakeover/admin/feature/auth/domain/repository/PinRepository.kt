package com.uniquemakeover.admin.feature.auth.domain.repository

import com.uniquemakeover.admin.feature.auth.domain.model.PinResult

interface PinRepository {
    suspend fun verifyPin(enteredPin: String): PinResult
    suspend fun isDefaultPin(): Boolean
    suspend fun changePin(newPin: String)
}
