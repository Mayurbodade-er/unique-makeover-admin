package com.uniquemakeover.admin.feature.appointments.presentation.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.PaymentMode
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.appointments.domain.usecase.RecordPaymentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordPaymentViewModel @Inject constructor(
    private val recordPaymentUseCase: RecordPaymentUseCase,
    private val appointmentRepository: AppointmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordPaymentUiState())
    val uiState: StateFlow<RecordPaymentUiState> = _uiState.asStateFlow()

    fun loadAppointment(appointment: Appointment) {
        val due = (appointment.rate - appointment.paid).coerceAtLeast(0)
        _uiState.update {
            it.copy(
                appointment = appointment,
                amountInput = if (due > 0) due.toString() else "",
                mode = PaymentMode.CASH,
                isOverpaymentConfirmationVisible = false,
                overpaymentPrompt = "",
                errorMessage = null,
                isSaved = false
            )
        }
    }

    fun loadAppointmentById(appointmentId: String) {
        viewModelScope.launch {
            val appointment = appointmentRepository.getAppointmentById(appointmentId)
            if (appointment != null) {
                loadAppointment(appointment)
            }
        }
    }

    fun onAmountChanged(amount: String) {
        val filtered = amount.filter { it.isDigit() }
        _uiState.update {
            it.copy(
                amountInput = filtered,
                errorMessage = null,
                isOverpaymentConfirmationVisible = false
            )
        }
    }

    fun onModeChanged(mode: PaymentMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun dismissOverpaymentPrompt() {
        _uiState.update { it.copy(isOverpaymentConfirmationVisible = false) }
    }

    fun recordPayment(confirmedOverpayment: Boolean = false) {
        val currentState = _uiState.value
        val appointment = currentState.appointment ?: return
        val amount = currentState.amountInput.toIntOrNull() ?: 0

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = recordPaymentUseCase(
                appointmentId = appointment.id,
                amount = amount,
                mode = currentState.mode,
                confirmedOverpayment = confirmedOverpayment
            )

            handlePaymentResult(result, confirmedOverpayment)
        }
    }

    private fun handlePaymentResult(result: DataResult<Unit>, confirmedOverpayment: Boolean) {
        when (result) {
            is DataResult.Success -> {
                _uiState.update {
                    it.copy(isLoading = false, isSaved = true, isOverpaymentConfirmationVisible = false)
                }
            }
            is DataResult.Error -> {
                val errorMsg = (result.message as? UiText.DynamicString)?.value ?: ""
                if (errorMsg.contains("exceeds balance due") && !confirmedOverpayment) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isOverpaymentConfirmationVisible = true,
                            overpaymentPrompt = errorMsg
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }
}
