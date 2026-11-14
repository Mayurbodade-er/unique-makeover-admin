package com.uniquemakeover.admin.feature.clients.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.clients.domain.usecase.GetClientHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ClientDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    appointmentRepository: AppointmentRepository,
    getClientHistoryUseCase: GetClientHistoryUseCase
) : ViewModel() {

    companion object {
        private const val STOP_TIMEOUT_MS = 5000L
    }

    val phone: String = savedStateHandle.get<String>("phone").orEmpty()

    private val _selectedTab = MutableStateFlow(0)

    val uiState: StateFlow<ClientDetailUiState> = combine(
        appointmentRepository.getAppointmentsByPhone(phone),
        getClientHistoryUseCase(phone),
        _selectedTab
    ) { appointments, historyItems, tab ->
        val latestName = appointments.maxByOrNull { it.bookedAt }?.name.orEmpty()
        val nonCancelledItems = historyItems.filter { it.status != AppointmentStatus.CANCEL }
        val spend = nonCancelledItems.sumOf { it.rate }
        val visits = nonCancelledItems.size

        ClientDetailUiState(
            isLoading = false,
            clientName = latestName,
            phone = phone,
            totalSpend = spend,
            totalVisits = visits,
            historyItems = historyItems,
            selectedTab = tab,
            errorMessage = null
        )
    }.catch { error ->
        emit(
            ClientDetailUiState(
                isLoading = false,
                phone = phone,
                errorMessage = error.localizedMessage ?: "Failed to load client history"
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = ClientDetailUiState(isLoading = true, phone = phone)
    )

    fun onTabSelected(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }
}
