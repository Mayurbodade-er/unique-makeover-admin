package com.uniquemakeover.admin.feature.appointments.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniquemakeover.admin.core.domain.model.Slot
import com.uniquemakeover.admin.core.util.TimeProvider
import com.uniquemakeover.admin.feature.appointments.domain.usecase.GenerateAvailableSlotsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppointmentsViewModel @Inject constructor(
    private val generateAvailableSlotsUseCase: GenerateAvailableSlotsUseCase,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppointmentsUiState())
    val uiState: StateFlow<AppointmentsUiState> = _uiState.asStateFlow()

    init {
        val today = timeProvider.todayDate()
        _uiState.update { it.copy(selectedDate = today) }
        loadSlotsForDate(today)
    }

    fun selectDate(date: String) {
        _uiState.update { it.copy(selectedDate = date) }
        loadSlotsForDate(date)
    }

    fun selectSlot(slot: Slot) {
        if (slot.isAvailable) {
            _uiState.update { it.copy(selectedSlot = slot) }
        }
    }

    private fun loadSlotsForDate(date: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val slots = generateAvailableSlotsUseCase(date)
            _uiState.update { state ->
                val currentSelected = state.selectedSlot
                val isStillAvailable = slots.any { it.time24 == currentSelected?.time24 && it.isAvailable }
                state.copy(
                    isLoading = false,
                    slots = slots,
                    selectedSlot = if (isStillAvailable) currentSelected else null
                )
            }
        }
    }
}
