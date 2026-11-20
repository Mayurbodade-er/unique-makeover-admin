package com.uniquemakeover.admin.feature.upi.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.util.UpiValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class UpiQrViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialAmount: Int = savedStateHandle.get<Int>("amount")
        ?: savedStateHandle.get<String>("amount")?.toIntOrNull() ?: 0
    private val initialClientName: String = savedStateHandle.get<String>("clientName") ?: ""

    private val _uiState = MutableStateFlow<UpiQrUiState>(UpiQrUiState.Loading)
    val uiState: StateFlow<UpiQrUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        settingsRepository.getSettings()
            .onEach { settings ->
                val upiId = settings?.upi?.trim().orEmpty()
                if (upiId.isEmpty() || !UpiValidator.isValid(upiId)) {
                    _uiState.update { UpiQrUiState.MissingConfiguration }
                } else {
                    val shopName = settings?.shop?.trim().orEmpty().ifEmpty { "Unique Makeover" }
                    val note = if (initialClientName.isNotBlank()) {
                        "$initialClientName - Salon Bill"
                    } else {
                        "Salon Payment"
                    }
                    _uiState.update {
                        UpiQrUiState.Content(
                            shopName = shopName,
                            upiId = upiId,
                            amount = initialAmount,
                            note = note
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}
