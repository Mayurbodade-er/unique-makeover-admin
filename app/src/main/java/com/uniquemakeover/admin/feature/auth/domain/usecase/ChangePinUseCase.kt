package com.uniquemakeover.admin.feature.auth.domain.usecase

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.feature.auth.domain.model.ChangePinResult
import com.uniquemakeover.admin.feature.auth.domain.model.PinResult
import com.uniquemakeover.admin.feature.auth.domain.repository.PinRepository
import javax.inject.Inject

private const val PIN_LENGTH = 4

class ChangePinUseCase @Inject constructor(
    private val pinRepository: PinRepository
) {
    suspend operator fun invoke(
        currentPin: String,
        newPin: String,
        confirmPin: String
    ): ChangePinResult {
        val validationError = validateInputs(currentPin, newPin, confirmPin)
        if (validationError != null) {
            return validationError
        }

        return when (val verifyResult = pinRepository.verifyPin(currentPin)) {
            is PinResult.Success -> {
                pinRepository.changePin(newPin)
                ChangePinResult.Success
            }
            is PinResult.Locked -> {
                ChangePinResult.Error(
                    UiText.DynamicString(
                        "Too many failed attempts. Try again in ${verifyResult.remainingSeconds}s"
                    )
                )
            }
            is PinResult.Invalid -> {
                ChangePinResult.IncorrectCurrentPin
            }
        }
    }

    private fun validateInputs(
        currentPin: String,
        newPin: String,
        confirmPin: String
    ): ChangePinResult? = when {
        currentPin.length != PIN_LENGTH || !currentPin.all(Char::isDigit) -> ChangePinResult.IncorrectCurrentPin
        newPin.length != PIN_LENGTH || !newPin.all(Char::isDigit) -> {
            ChangePinResult.Error(UiText.DynamicString("New PIN must be 4 digits"))
        }
        currentPin == newPin -> ChangePinResult.SameAsCurrentPin
        newPin != confirmPin -> ChangePinResult.PinsDoNotMatch
        else -> null
    }

    fun isWeakPin(pin: String): Boolean {
        if (pin.length != PIN_LENGTH || !pin.all(Char::isDigit)) return false
        val allSame = pin.all { it == pin[0] }
        val sequentialAsc = pin in listOf("0123", "1234", "2345", "3456", "4567", "5678", "6789")
        val sequentialDesc = pin in listOf("9876", "8765", "7654", "6543", "5432", "4321", "3210")
        return allSame || sequentialAsc || sequentialDesc
    }
}
