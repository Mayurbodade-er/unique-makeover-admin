package com.uniquemakeover.admin.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniquemakeover.admin.core.util.TimeProvider
import com.uniquemakeover.admin.feature.dashboard.domain.usecase.GetDashboardKpisUseCase
import com.uniquemakeover.admin.feature.dashboard.domain.usecase.GetTodayScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardKpisUseCase: GetDashboardKpisUseCase,
    private val getTodayScheduleUseCase: GetTodayScheduleUseCase,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun refresh() {
        loadDashboardData()
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
    }
}
