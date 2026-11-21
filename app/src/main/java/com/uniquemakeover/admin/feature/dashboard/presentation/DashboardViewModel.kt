package com.uniquemakeover.admin.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.core.domain.model.StatusUpdateResult
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.TimeProvider
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.usecase.AppointmentOperations
import com.uniquemakeover.admin.feature.dashboard.domain.usecase.GetDashboardKpisUseCase
import com.uniquemakeover.admin.feature.dashboard.domain.usecase.GetTodayScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardKpisUseCase: GetDashboardKpisUseCase,
    private val getTodayScheduleUseCase: GetTodayScheduleUseCase,
    private val appointmentOperations: AppointmentOperations,
    private val timeProvider: TimeProvider,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<DashboardUiEffect>()
    val uiEffect: SharedFlow<DashboardUiEffect> = _uiEffect.asSharedFlow()

    init {
        loadDashboardData()
    }

    fun refresh() {
        loadDashboardData()
    }

    fun markDone(appointment: Appointment) {
        viewModelScope.launch {
            val previousStatus = AppointmentStatus.fromValue(appointment.status)
            when (val result = appointmentOperations.markDone(appointment.id)) {
                is DataResult.Success -> {
                    _uiEffect.emit(
                        DashboardUiEffect.ShowUndoSnackbar(
                            message = UiText.DynamicString("Marked ${appointment.name} as Done"),
                            appointmentId = appointment.id,
                            previousStatus = previousStatus
                        )
                    )
                }
                is DataResult.Error -> {
                    _uiEffect.emit(DashboardUiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    fun cancel(appointment: Appointment) {
        viewModelScope.launch {
            val previousStatus = AppointmentStatus.fromValue(appointment.status)
            when (val result = appointmentOperations.cancel(appointment.id)) {
                is DataResult.Success -> {
                    _uiEffect.emit(
                        DashboardUiEffect.ShowUndoSnackbar(
                            message = UiText.DynamicString("Cancelled appointment for ${appointment.name}"),
                            appointmentId = appointment.id,
                            previousStatus = previousStatus
                        )
                    )
                }
                is DataResult.Error -> {
                    _uiEffect.emit(DashboardUiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    fun undoStatus(appointmentId: String, previousStatus: AppointmentStatus) {
        viewModelScope.launch {
            when (val result = appointmentOperations.undo(appointmentId, previousStatus)) {
                is StatusUpdateResult.Success -> {
                    _uiEffect.emit(
                        DashboardUiEffect.ShowMessage(
                            UiText.DynamicString("Status reverted to ${previousStatus.value}")
                        )
                    )
                }
                is StatusUpdateResult.SlotConflict -> _uiEffect.emit(DashboardUiEffect.ShowMessage(result.message))
                is StatusUpdateResult.InvalidTransition -> _uiEffect.emit(DashboardUiEffect.ShowMessage(result.message))
                is StatusUpdateResult.NotFound -> _uiEffect.emit(DashboardUiEffect.ShowMessage(result.message))
            }
        }
    }

    fun deleteAppointment(appointment: Appointment) {
        viewModelScope.launch {
            when (val result = appointmentOperations.delete(appointment.id)) {
                is DataResult.Success -> {
                    _uiEffect.emit(
                        DashboardUiEffect.ShowMessage(
                            UiText.DynamicString("Deleted appointment for ${appointment.name}")
                        )
                    )
                }
                is DataResult.Error -> {
                    _uiEffect.emit(DashboardUiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun loadDashboardData() {
        val today = timeProvider.todayDate()
        _uiState.update { it.copy(todayDate = today, isLoading = true) }

        getDashboardKpisUseCase()
            .onEach { kpis ->
                _uiState.update { it.copy(kpis = kpis, isLoading = false) }
            }
            .launchIn(viewModelScope)

        getTodayScheduleUseCase()
            .onEach { schedule ->
                _uiState.update { it.copy(schedule = schedule, isLoading = false) }
            }
            .launchIn(viewModelScope)

        settingsRepository.getSettings()
            .onEach { settings ->
                if (settings != null) {
                    _uiState.update {
                        it.copy(
                            shopName = settings.shop.ifBlank { "Unique Makeover" },
                            tagline = settings.tagline.ifBlank { "Luxury Beauty Parlour" },
                            logoPath = settings.logoPath
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}
