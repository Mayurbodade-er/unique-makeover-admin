package com.uniquemakeover.admin.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.feature.auth.domain.model.LockoutState
import com.uniquemakeover.admin.feature.auth.domain.model.PinResult
import com.uniquemakeover.admin.feature.auth.domain.usecase.GetLockoutStateUseCase
import com.uniquemakeover.admin.feature.auth.domain.usecase.VerifyPinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MAX_PIN_DIGITS = 4

@HiltViewModel
class PinLockViewModel @Inject constructor(
    private val verifyPinUseCase: VerifyPinUseCase,
    private val getLockoutStateUseCase: GetLockoutStateUseCase
) : ViewModel() {

    private val pinBuffer = StringBuilder()

    private val _uiState = MutableStateFlow(PinLockUiState())
    val uiState: StateFlow<PinLockUiState> = _uiState.asStateFlow()

    init {
        observeLockout()
    }

    private fun observeLockout() {
        viewModelScope.launch {
            getLockoutStateUseCase().collect { lockoutState ->
                _uiState.update { current ->
                    if (lockoutState is LockoutState.Locked) {
                        pinBuffer.clear()
                        current.copy(
                            lockoutState = lockoutState,
                            digitsEntered = 0,
                            isError = true,
                            errorMessage = UiText.DynamicString(
                                "Too many failed attempts. Try again in ${lockoutState.remainingSeconds}s"
                            )
                        )
                    } else {
                        val wasLocked = current.lockoutState is LockoutState.Locked
                        current.copy(
                            lockoutState = LockoutState.Unlocked,
                            isError = if (wasLocked) false else current.isError,
                            errorMessage = if (wasLocked) null else current.errorMessage
                        )
                    }
                }
            }
        }
    }

    fun onDigitClick(digit: Char) {
        if (_uiState.value.isLocked || !digit.isDigit() || pinBuffer.length >= MAX_PIN_DIGITS) {
            return
        }

        if (_uiState.value.isError) {
            pinBuffer.clear()
            _uiState.update { it.copy(isError = false, errorMessage = null) }
        }

        pinBuffer.append(digit)
        val currentCount = pinBuffer.length
        _uiState.update { it.copy(digitsEntered = currentCount) }

        if (currentCount == MAX_PIN_DIGITS) {
            submitPin()
        }
    }

    fun onBackspace() {
        if (_uiState.value.isLocked) return
        if (pinBuffer.isNotEmpty()) {
            pinBuffer.deleteCharAt(pinBuffer.length - 1)
            _uiState.update {
                it.copy(
                    digitsEntered = pinBuffer.length,
                    isError = false,
                    errorMessage = null
                )
            }
        }
    }

    fun onClear() {
        if (_uiState.value.isLocked) return
        pinBuffer.clear()
        _uiState.update {
            it.copy(
                digitsEntered = 0,
                isError = false,
                errorMessage = null
            )
        }
    }

    private fun submitPin() {
        val enteredPin = pinBuffer.toString()
        viewModelScope.launch {
            val result = verifyPinUseCase(enteredPin)
            handleVerificationResult(result)
        }
    }

    private fun handleVerificationResult(result: PinResult) {
        when (result) {
            is PinResult.Success -> {
                _uiState.update {
                    it.copy(
                        isAuthenticated = true,
                        isError = false,
                        errorMessage = null
                    )
                }
            }
            is PinResult.Invalid -> {
                pinBuffer.clear()
                _uiState.update {
                    it.copy(
                        digitsEntered = 0,
                        isError = true,
                        errorMessage = UiText.DynamicString("Incorrect PIN. Try again."),
                        shakeTrigger = System.currentTimeMillis()
                    )
                }
            }
            is PinResult.Locked -> {
                pinBuffer.clear()
                _uiState.update {
                    it.copy(
                        digitsEntered = 0,
                        isError = true,
                        lockoutState = LockoutState.Locked(result.remainingSeconds),
                        errorMessage = UiText.DynamicString(
                            "Too many failed attempts. Try again in ${result.remainingSeconds}s"
                        ),
                        shakeTrigger = System.currentTimeMillis()
                    )
                }
            }
        }
    }
}
