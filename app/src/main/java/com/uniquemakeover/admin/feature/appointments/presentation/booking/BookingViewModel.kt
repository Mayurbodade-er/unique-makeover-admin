package com.uniquemakeover.admin.feature.appointments.presentation.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.domain.model.Slot
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.TimeProvider
import com.uniquemakeover.admin.feature.appointments.domain.usecase.BookAppointmentUseCase
import com.uniquemakeover.admin.feature.appointments.domain.usecase.GenerateAvailableSlotsUseCase
import com.uniquemakeover.admin.feature.services.domain.usecase.GetServicesUseCase
import com.uniquemakeover.admin.feature.settings.domain.usecase.GetOperationsConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val getServicesUseCase: GetServicesUseCase,
    private val getOperationsConfigUseCase: GetOperationsConfigUseCase,
    private val generateAvailableSlotsUseCase: GenerateAvailableSlotsUseCase,
    private val bookAppointmentUseCase: BookAppointmentUseCase,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        val today = timeProvider.todayDate()
        _uiState.update { it.copy(isLoading = true, draft = it.draft.copy(date = today)) }
        viewModelScope.launch {
            val services = getServicesUseCase().first()
            val config = getOperationsConfigUseCase()
            val slots = generateAvailableSlotsUseCase(today)
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    availableServices = services,
                    staffList = config.staffList,
                    availableSlots = slots,
                    draft = state.draft.copy(staff = config.staffList.firstOrNull() ?: "Sunita")
                )
            }
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(draft = it.draft.copy(name = name), nameError = null) }
    }

    fun updatePhone(phone: String) {
        _uiState.update { it.copy(draft = it.draft.copy(phone = phone), phoneError = null) }
    }

    fun toggleService(service: Service) {
        _uiState.update { state ->
            val currentList = state.draft.selectedServices
            val newList = if (currentList.any { it.id == service.id }) {
                currentList.filterNot { it.id == service.id }
            } else {
                currentList + service
            }
            val newDraft = state.draft.copy(selectedServices = newList)
            val financialError = validateFinancialClamp(newDraft.discount, newDraft.advancePaid, newDraft.subtotal)
            state.copy(draft = newDraft, serviceError = null, financialError = financialError)
        }
    }

    fun selectDate(date: String) {
        _uiState.update { it.copy(draft = it.draft.copy(date = date, time = ""), slotError = null) }
        viewModelScope.launch {
            val slots = generateAvailableSlotsUseCase(date)
            _uiState.update { it.copy(availableSlots = slots) }
        }
    }

    fun selectSlot(slot: Slot) {
        if (slot.isAvailable) {
            _uiState.update { it.copy(draft = it.draft.copy(time = slot.time24), slotError = null) }
        }
    }

    fun selectStaff(staff: String) {
        _uiState.update { it.copy(draft = it.draft.copy(staff = staff)) }
    }

    fun updateDiscount(discount: Int) {
        _uiState.update { state ->
            val newDraft = state.draft.copy(discount = discount)
            val error = validateFinancialClamp(discount, newDraft.advancePaid, newDraft.subtotal)
            state.copy(draft = newDraft, financialError = error)
        }
    }

    fun updateAdvance(advance: Int) {
        _uiState.update { state ->
            val newDraft = state.draft.copy(advancePaid = advance)
            val error = validateFinancialClamp(newDraft.discount, advance, newDraft.subtotal)
            state.copy(draft = newDraft, financialError = error)
        }
    }

    fun updateNote(note: String) {
        _uiState.update { it.copy(draft = it.draft.copy(internalNote = note)) }
    }

    fun saveAppointment(onSuccess: () -> Unit = {}) {
        val draft = _uiState.value.draft
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, generalError = null) }
            when (val result = bookAppointmentUseCase(draft)) {
                is DataResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, bookingSuccess = true) }
                    onSuccess()
                }
                is DataResult.Error -> {
                    val errorStr = result.message.toString()
                    _uiState.update { it.copy(isSaving = false, generalError = errorStr) }
                }
            }
        }
    }

    private fun validateFinancialClamp(discount: Int, advance: Int, subtotal: Int): String? {
        val finalRate = (subtotal - discount).coerceAtLeast(0)
        return when {
            discount > subtotal -> "Discount cannot exceed subtotal (₹$subtotal)"
            advance > finalRate -> "Advance cannot exceed total bill amount (₹$finalRate)"
            else -> null
        }
    }
}
