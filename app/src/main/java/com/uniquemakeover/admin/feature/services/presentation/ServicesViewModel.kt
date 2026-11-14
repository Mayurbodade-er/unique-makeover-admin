package com.uniquemakeover.admin.feature.services.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.TimeProvider
import com.uniquemakeover.admin.feature.appointments.domain.repository.AppointmentRepository
import com.uniquemakeover.admin.feature.services.domain.model.ServiceDraft
import com.uniquemakeover.admin.feature.services.domain.usecase.GetServicesUseCase
import com.uniquemakeover.admin.feature.services.domain.usecase.ServiceOperations
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

sealed interface ServicesUiEffect {
    data class ShowMessage(val message: UiText) : ServicesUiEffect
}

@HiltViewModel
class ServicesViewModel @Inject constructor(
    private val getServicesUseCase: GetServicesUseCase,
    private val serviceOperations: ServiceOperations,
    private val appointmentRepository: AppointmentRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServicesUiState(isLoading = true))
    val uiState: StateFlow<ServicesUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ServicesUiEffect>()
    val uiEffect: SharedFlow<ServicesUiEffect> = _uiEffect.asSharedFlow()

    init {
        observeServices()
    }

    private fun observeServices() {
        getServicesUseCase()
            .onEach { services ->
                _uiState.update { it.copy(isLoading = false, services = services, errorMessage = null) }
            }
            .launchIn(viewModelScope)
    }

    fun openAddSheet() {
        _uiState.update {
            it.copy(
                isEditorOpen = true,
                editingService = null,
                editorName = "",
                editorRate = "",
                editorError = null
            )
        }
    }

    fun openEditSheet(serviceId: String) {
        val service = _uiState.value.services.find { it.id == serviceId } ?: return
        _uiState.update {
            it.copy(
                isEditorOpen = true,
                editingService = service,
                editorName = service.name,
                editorRate = service.rate.toString(),
                editorError = null
            )
        }
    }

    fun closeEditor() {
        _uiState.update {
            it.copy(
                isEditorOpen = false,
                editingService = null,
                editorError = null
            )
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(editorName = name, editorError = null) }
    }

    fun onRateChange(rate: String) {
        val filtered = rate.filter { it.isDigit() }
        _uiState.update { it.copy(editorRate = filtered, editorError = null) }
    }

    fun saveService() {
        val state = _uiState.value
        val rateInt = state.editorRate.toIntOrNull() ?: 0

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = if (state.isEditMode) {
                val updated = state.editingService!!.copy(
                    name = state.editorName,
                    rate = rateInt
                )
                serviceOperations.update(updated)
            } else {
                val draft = ServiceDraft(
                    name = state.editorName,
                    rate = rateInt
                )
                serviceOperations.add(draft)
            }
            handleSaveResult(result)
        }
    }

    private suspend fun handleSaveResult(result: DataResult<*>) {
        _uiState.update { it.copy(isSaving = false) }
        when (result) {
            is DataResult.Success -> {
                closeEditor()
                _uiEffect.emit(ServicesUiEffect.ShowMessage(UiText.DynamicString("Service saved successfully")))
            }
            is DataResult.Error -> {
                _uiState.update { it.copy(editorError = result.message) }
            }
        }
    }

    fun requestDelete(service: Service) {
        viewModelScope.launch {
            val count = appointmentRepository.countUpcomingAppointmentsForService(
                serviceId = service.id,
                today = timeProvider.todayDate()
            )
            _uiState.update {
                it.copy(
                    deleteTarget = service,
                    upcomingCountForDelete = count
                )
            }
        }
    }

    fun confirmDelete() {
        val target = _uiState.value.deleteTarget ?: return
        viewModelScope.launch {
            when (val result = serviceOperations.delete(target.id)) {
                is DataResult.Success -> {
                    _uiState.update {
                        it.copy(
                            deleteTarget = null,
                            upcomingCountForDelete = 0,
                            isEditorOpen = false,
                            editingService = null
                        )
                    }
                    _uiEffect.emit(
                        ServicesUiEffect.ShowMessage(
                            UiText.DynamicString("Deleted '${target.name}'")
                        )
                    )
                }
                is DataResult.Error -> {
                    _uiEffect.emit(ServicesUiEffect.ShowMessage(result.message))
                }
            }
        }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(deleteTarget = null, upcomingCountForDelete = 0) }
    }
}
