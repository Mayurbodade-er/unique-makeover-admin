package com.uniquemakeover.admin.feature.appointments.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniquemakeover.admin.core.domain.model.Slot
import com.uniquemakeover.admin.core.util.TimeProvider
import com.uniquemakeover.admin.feature.appointments.domain.model.AppointmentFilter
import com.uniquemakeover.admin.feature.appointments.domain.model.DateRange
import com.uniquemakeover.admin.feature.appointments.domain.model.FilterTab
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.appointments.domain.usecase.GenerateAvailableSlotsUseCase
import com.uniquemakeover.admin.feature.appointments.domain.usecase.GetAppointmentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AppointmentsViewModel @Inject constructor(
    private val generateAvailableSlotsUseCase: GenerateAvailableSlotsUseCase,
    private val getAppointmentsUseCase: GetAppointmentsUseCase,
    private val appointmentRepository: AppointmentRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppointmentsUiState())
    val uiState: StateFlow<AppointmentsUiState> = _uiState.asStateFlow()

    private val _filter = MutableStateFlow(AppointmentFilter())

    init {
        val today = timeProvider.todayDate()
        _uiState.update { it.copy(selectedDate = today) }
        loadSlotsForDate(today)
        observeFilteredAppointments()
        observeTabCounts()
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

    fun updateTab(tab: FilterTab) {
        _filter.update { it.copy(tab = tab) }
        _uiState.update { it.copy(filter = _filter.value) }
    }

    fun updateSearchQuery(query: String) {
        _filter.update { it.copy(searchQuery = query) }
        _uiState.update { it.copy(filter = _filter.value) }
    }

    fun updateDateRange(dateRange: DateRange?) {
        _filter.update { it.copy(dateRange = dateRange) }
        _uiState.update { it.copy(filter = _filter.value) }
    }

    fun clearFilters() {
        _filter.update { AppointmentFilter(tab = FilterTab.ALL, dateRange = null, searchQuery = "") }
        _uiState.update { it.copy(filter = _filter.value) }
    }

    private fun observeFilteredAppointments() {
        _filter
            .flatMapLatest { filter ->
                getAppointmentsUseCase(filter)
            }
            .onEach { appointments ->
                _uiState.update { it.copy(appointments = appointments, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeTabCounts() {
        val today = timeProvider.todayDate()
        appointmentRepository.getAllAppointments()
            .onEach { allList ->
                val upcoming = allList.count { it.status == "booked" && it.date >= today }
                val completed = allList.count { it.status == "done" }
                val cancelled = allList.count { it.status.lowercase() in listOf("cancel", "cancelled") }
                val counts = TabCounts(
                    all = allList.size,
                    upcoming = upcoming,
                    completed = completed,
                    cancelled = cancelled
                )
                _uiState.update { it.copy(tabCounts = counts) }
            }
            .launchIn(viewModelScope)
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
