package com.uniquemakeover.admin.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.feature.auth.domain.model.PinResult
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
    private val verifyPinUseCase: VerifyPinUseCase
) : ViewModel() {

    private val pinBuffer = StringBuilder()

    private val _uiState = MutableStateFlow(PinLockUiState())
    val uiState: StateFlow<PinLockUiState> = _uiState.asStateFlow()

    fun onDigitClick(digit: Char) {
        if (!digit.isDigit()) return
        if (pinBuffer.length >= MAX_PIN_DIGITS) return

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
            when (verifyPinUseCase(enteredPin)) {
                is PinResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isAuthenticated = true,
                            isError = false,
                            errorMessage = null
                        )
                    }
                }
                is PinResult.Invalid, is PinResult.Locked -> {
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
            }
        }
    }
}
