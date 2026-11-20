package com.uniquemakeover.admin.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.settings.domain.usecase.GetCommunicationConfigUseCase
import com.uniquemakeover.admin.feature.settings.domain.usecase.GetOperationsConfigUseCase
import com.uniquemakeover.admin.feature.settings.domain.usecase.UpdateCommunicationUseCase
import com.uniquemakeover.admin.feature.settings.domain.usecase.UpdateOperationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getOperationsConfigUseCase: GetOperationsConfigUseCase,
    private val updateOperationsUseCase: UpdateOperationsUseCase,
    private val getCommunicationConfigUseCase: GetCommunicationConfigUseCase,
    private val updateCommunicationUseCase: UpdateCommunicationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val operationsConfig = getOperationsConfigUseCase()
            val commsConfig = getCommunicationConfigUseCase()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    operationsConfig = operationsConfig,
                    communicationConfig = commsConfig
                )
            }
        }
    }

    fun saveOperations(
        openTime: String,
        closeTime: String,
        slotMinutes: Int,
        staffRaw: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null, successMessage = null) }
            when (val result = updateOperationsUseCase(openTime, closeTime, slotMinutes, staffRaw)) {
                is DataResult.Success -> {
                    val updatedConfig = getOperationsConfigUseCase()
                    _uiState.update {
                        it.copy(
                            operationsConfig = updatedConfig,
                            successMessage = UiText.DynamicString("Operations settings saved"),
                            errorMessage = null
                        )
                    }
                }
                is DataResult.Error -> {
                    _uiState.update {
                        it.copy(
                            errorMessage = result.message,
                            successMessage = null
                        )
                    }
                }
            }
        }
    }

    fun saveCommunication(
        whatsappPhone: String,
        merchantUpiId: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(commsErrorMessage = null, successMessage = null) }
            when (val result = updateCommunicationUseCase(whatsappPhone, merchantUpiId)) {
                is DataResult.Success -> {
                    val updatedConfig = getCommunicationConfigUseCase()
                    _uiState.update {
                        it.copy(
                            communicationConfig = updatedConfig,
                            successMessage = UiText.DynamicString("Communication settings saved"),
                            commsErrorMessage = null
                        )
                    }
                }
                is DataResult.Error -> {
                    _uiState.update {
                        it.copy(
                            commsErrorMessage = result.message,
                            successMessage = null
                        )
                    }
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, commsErrorMessage = null, successMessage = null) }
    }
}
