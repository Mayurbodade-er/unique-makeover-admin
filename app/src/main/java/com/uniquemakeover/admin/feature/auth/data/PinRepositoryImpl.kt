package com.uniquemakeover.admin.feature.auth.data

import com.uniquemakeover.admin.core.di.IoDispatcher
import com.uniquemakeover.admin.core.security.PinLockManager
import com.uniquemakeover.admin.feature.auth.domain.model.PinResult
import com.uniquemakeover.admin.feature.auth.domain.repository.PinRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinRepositoryImpl @Inject constructor(
    private val pinLockManager: PinLockManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PinRepository {

    override suspend fun verifyPin(enteredPin: String): PinResult {
        return withContext(ioDispatcher) {
            if (pinLockManager.verifyPin(enteredPin)) {
                PinResult.Success
            } else {
                PinResult.Invalid
            }
        }
    }

    override suspend fun isDefaultPin(): Boolean {
        return withContext(ioDispatcher) {
            pinLockManager.isDefaultPin()
        }
    }

    override suspend fun changePin(newPin: String) {
        withContext(ioDispatcher) {
            pinLockManager.setPin(newPin)
        }
    }
}
