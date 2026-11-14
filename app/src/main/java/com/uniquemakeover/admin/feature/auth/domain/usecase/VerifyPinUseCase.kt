package com.uniquemakeover.admin.feature.auth.domain.usecase

import com.uniquemakeover.admin.feature.auth.domain.model.PinResult
import com.uniquemakeover.admin.feature.auth.domain.repository.PinRepository
import javax.inject.Inject

private const val PIN_LENGTH = 4

class VerifyPinUseCase @Inject constructor(
    private val pinRepository: PinRepository
) {
    suspend operator fun invoke(pin: String): PinResult {
        if (pin.length != PIN_LENGTH || !pin.all { it.isDigit() }) {
            return PinResult.Invalid
        }
        return pinRepository.verifyPin(pin)
    }
}
