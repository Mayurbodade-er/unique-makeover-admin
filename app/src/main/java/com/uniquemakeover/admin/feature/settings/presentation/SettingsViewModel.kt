package com.uniquemakeover.admin.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.settings.domain.model.BrandingConfig
import com.uniquemakeover.admin.feature.settings.domain.repository.BrandingStorageRepository
import com.uniquemakeover.admin.feature.settings.domain.usecase.SettingsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val BYTES_PER_KB = 1024

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsUseCases: SettingsUseCases,
    private val brandingStorageRepository: BrandingStorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val branding = settingsUseCases.getBrandingConfig()
            val operationsConfig = settingsUseCases.getOperationsConfig()
            val commsConfig = settingsUseCases.getCommunicationConfig()
            val template = settingsUseCases.getReminderTemplate()
            val lastBackup = settingsUseCases.getLastBackup()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    brandingConfig = branding,
                    operationsConfig = operationsConfig,
                    communicationConfig = commsConfig,
                    reminderTemplate = template,
                    lastBackup = lastBackup
                )
            }
        }
    }

    fun saveBranding(draft: BrandingDraft) {
        viewModelScope.launch {
            _uiState.update { it.copy(brandingErrorMessage = null, successMessage = null) }
            var currentLogoPath = _uiState.value.brandingConfig.logoPath
            if (draft.removeLogo) {
                brandingStorageRepository.deleteLogo()
                currentLogoPath = null
            } else if (draft.logoUriString != null) {
                when (val result = brandingStorageRepository.saveLogo(draft.logoUriString)) {
                    is DataResult.Success -> currentLogoPath = result.data
                    is DataResult.Error -> {
                        _uiState.update { it.copy(brandingErrorMessage = result.message) }
                        return@launch
                    }
                }
            }

            var currentWallpaperPath = _uiState.value.brandingConfig.wallpaperPath
            if (draft.removeWallpaper) {
                brandingStorageRepository.deleteWallpaper()
                currentWallpaperPath = null
            } else if (draft.wallpaperUriString != null) {
                when (val result = brandingStorageRepository.saveWallpaper(draft.wallpaperUriString)) {
                    is DataResult.Success -> currentWallpaperPath = result.data
                    is DataResult.Error -> {
                        _uiState.update { it.copy(brandingErrorMessage = result.message) }
                        return@launch
                    }
                }
            }

            val updatedConfig = BrandingConfig(
                shopName = draft.shopName.trim(),
                tagline = draft.tagline.trim(),
                logoPath = currentLogoPath,
                wallpaperPath = currentWallpaperPath
            )

            when (val result = settingsUseCases.updateBranding(updatedConfig)) {
                is DataResult.Success -> {
                    _uiState.update {
                        it.copy(
                            brandingConfig = updatedConfig,
                            successMessage = UiText.DynamicString("Branding settings saved"),
                            brandingErrorMessage = null
                        )
                    }
                }
                is DataResult.Error -> {
                    _uiState.update {
                        it.copy(brandingErrorMessage = result.message)
                    }
                }
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
            when (val result = settingsUseCases.updateOperations(openTime, closeTime, slotMinutes, staffRaw)) {
                is DataResult.Success -> {
                    val updatedConfig = settingsUseCases.getOperationsConfig()
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
            when (val result = settingsUseCases.updateCommunication(whatsappPhone, merchantUpiId)) {
                is DataResult.Success -> {
                    val updatedConfig = settingsUseCases.getCommunicationConfig()
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

    fun saveReminderTemplate(template: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(templateErrorMessage = null, successMessage = null) }
            when (val result = settingsUseCases.updateReminderTemplate(template)) {
                is DataResult.Success -> {
                    val updatedTemplate = settingsUseCases.getReminderTemplate()
                    _uiState.update {
                        it.copy(
                            reminderTemplate = updatedTemplate,
                            successMessage = UiText.DynamicString("Reminder template saved"),
                            templateErrorMessage = null
                        )
                    }
                }
                is DataResult.Error -> {
                    _uiState.update {
                        it.copy(
                            templateErrorMessage = result.message,
                            successMessage = null
                        )
                    }
                }
            }
        }
    }

    fun changePin(
        currentPin: String,
        newPin: String,
        confirmPin: String
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    pinErrorMessage = null,
                    successMessage = null,
                    isChangePinSuccess = false
                )
            }
            val result = settingsUseCases.changePin(currentPin, newPin, confirmPin)
            handlePinResult(result)
        }
    }

    private fun handlePinResult(result: com.uniquemakeover.admin.feature.auth.domain.model.ChangePinResult) {
        when (result) {
            is com.uniquemakeover.admin.feature.auth.domain.model.ChangePinResult.Success -> {
                _uiState.update {
                    it.copy(
                        successMessage = UiText.DynamicString("PIN changed successfully"),
                        pinErrorMessage = null,
                        isChangePinSuccess = true
                    )
                }
            }
            is com.uniquemakeover.admin.feature.auth.domain.model.ChangePinResult.IncorrectCurrentPin -> {
                _uiState.update {
                    it.copy(pinErrorMessage = UiText.DynamicString("Incorrect current PIN"))
                }
            }
            is com.uniquemakeover.admin.feature.auth.domain.model.ChangePinResult.PinsDoNotMatch -> {
                _uiState.update {
                    it.copy(pinErrorMessage = UiText.DynamicString("PINs do not match"))
                }
            }
            is com.uniquemakeover.admin.feature.auth.domain.model.ChangePinResult.SameAsCurrentPin -> {
                _uiState.update {
                    it.copy(pinErrorMessage = UiText.DynamicString("New PIN must be different from current PIN"))
                }
            }
            is com.uniquemakeover.admin.feature.auth.domain.model.ChangePinResult.Error -> {
                _uiState.update {
                    it.copy(pinErrorMessage = result.message)
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                commsErrorMessage = null,
                templateErrorMessage = null,
                brandingErrorMessage = null,
                pinErrorMessage = null,
                successMessage = null,
                isChangePinSuccess = false
            )
        }
    }

    fun exportBackup(targetUriString: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingBackup = true, errorMessage = null) }
            when (val result = settingsUseCases.exportBackup(targetUriString)) {
                is DataResult.Success -> {
                    val sizeKb = result.data / BYTES_PER_KB
                    val updatedLastBackup = settingsUseCases.getLastBackup()
                    _uiState.update {
                        it.copy(
                            isExportingBackup = false,
                            successMessage = UiText.DynamicString("Backup exported successfully (${sizeKb} KB)"),
                            lastBackup = updatedLastBackup
                        )
                    }
                }
                is DataResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isExportingBackup = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }
}
