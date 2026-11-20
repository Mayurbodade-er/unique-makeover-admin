package com.uniquemakeover.admin.feature.upi.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.UpiValidator
import com.uniquemakeover.admin.feature.upi.domain.launcher.UpiWhatsAppLauncher
import com.uniquemakeover.admin.feature.upi.domain.model.PaymentPreset
import com.uniquemakeover.admin.feature.upi.domain.model.UpiRequest
import com.uniquemakeover.admin.feature.upi.domain.usecase.ComputePresetAmountUseCase
import com.uniquemakeover.admin.feature.upi.domain.usecase.UpiUseCases
import com.uniquemakeover.admin.feature.upi.presentation.mapper.toBitmap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

private const val ADVANCE_RATIO = 0.30

@HiltViewModel
class UpiQrViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val upiUseCases: UpiUseCases,
    private val upiWhatsAppLauncher: UpiWhatsAppLauncher,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val argAmount: Int = savedStateHandle.get<Int>("amount")
        ?: savedStateHandle.get<String>("amount")?.toIntOrNull() ?: 0
    private val rate: Int = savedStateHandle.get<Int>("rate")
        ?: savedStateHandle.get<String>("rate")?.toIntOrNull() ?: argAmount
    private val paid: Int = savedStateHandle.get<Int>("paid")
        ?: savedStateHandle.get<String>("paid")?.toIntOrNull() ?: 0
    private val clientName: String = savedStateHandle.get<String>("clientName") ?: ""
    private val phone: String = savedStateHandle.get<String>("phone") ?: ""
    private val services: String = savedStateHandle.get<String>("services") ?: ""

    private val isAdvanceDisabled: Boolean
        get() {
            val advance30 = (rate * ADVANCE_RATIO).roundToInt()
            return paid >= advance30 && advance30 > 0
        }

    private val initialAmount: Int = if (argAmount > 0) {
        argAmount
    } else {
        (rate - paid).coerceAtLeast(0)
    }

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
                    val note = if (clientName.isNotBlank()) {
                        "$clientName - Salon Bill"
                    } else {
                        "Salon Payment"
                    }
                    generateContent(shopName, upiId, initialAmount, note)
                }
            }
            .launchIn(viewModelScope)
    }

    private suspend fun generateContent(
        shopName: String,
        upiId: String,
        amount: Int,
        note: String
    ) {
        val upiRequest = UpiRequest(
            upiId = upiId,
            payeeName = shopName,
            amount = amount,
            transactionNote = note
        )
        val upiUri = upiUseCases.buildUpiUri(upiRequest)
        val qrMatrix = upiUseCases.generateQrMatrix(upiUri)
        val qrBitmap = qrMatrix.toBitmap()

        _uiState.update {
            UpiQrUiState.Content(
                qrBitmap = qrBitmap,
                qrMatrix = qrMatrix,
                upiUri = upiUri,
                shopName = shopName,
                upiId = upiId,
                amount = amount,
                note = note,
                selectedPreset = PaymentPreset.BalanceDue,
                isAdvanceDisabled = isAdvanceDisabled
            )
        }
    }

    fun onPresetSelected(preset: PaymentPreset) {
        val content = _uiState.value as? UpiQrUiState.Content ?: return
        val newAmount = upiUseCases.computePresetAmount(rate, paid, preset)
        val customInput = if (preset is PaymentPreset.Custom) {
            if (preset.customAmount > 0) preset.customAmount.toString() else ""
        } else {
            ""
        }
        regenerateQr(content, newAmount, preset, customInput)
    }

    fun onCustomAmountChanged(input: String) {
        val content = _uiState.value as? UpiQrUiState.Content ?: return
        val digits = input.filter { it.isDigit() }
        val customAmount = digits.toIntOrNull()?.coerceIn(
            ComputePresetAmountUseCase.MIN_AMOUNT,
            ComputePresetAmountUseCase.MAX_AMOUNT
        ) ?: 0
        val preset = PaymentPreset.Custom(customAmount)
        regenerateQr(content, customAmount, preset, digits)
    }

    private fun regenerateQr(
        content: UpiQrUiState.Content,
        newAmount: Int,
        preset: PaymentPreset,
        customInput: String
    ) {
        viewModelScope.launch {
            val upiRequest = UpiRequest(
                upiId = content.upiId,
                payeeName = content.shopName,
                amount = newAmount,
                transactionNote = content.note
            )
            val upiUri = upiUseCases.buildUpiUri(upiRequest)
            val qrMatrix = upiUseCases.generateQrMatrix(upiUri)
            val qrBitmap = qrMatrix.toBitmap()

            _uiState.update {
                content.copy(
                    qrBitmap = qrBitmap,
                    qrMatrix = qrMatrix,
                    upiUri = upiUri,
                    amount = newAmount,
                    selectedPreset = preset,
                    customAmountInput = customInput
                )
            }
        }
    }

    suspend fun saveToGallery(): DataResult<String> {
        val content = _uiState.value as? UpiQrUiState.Content
        val matrix = content?.qrMatrix
        if (content == null || matrix == null) {
            return DataResult.Error(UiText.DynamicString("QR Code not available"))
        }
        val filename = if (content.amount > 0) {
            "UPI_QR_${content.amount}"
        } else {
            "UPI_QR_UniqueMakeover"
        }
        return upiUseCases.saveQrPng(matrix, filename)
    }

    suspend fun shareQr(): DataResult<Unit> {
        val content = _uiState.value as? UpiQrUiState.Content
        val matrix = content?.qrMatrix
        if (content == null || matrix == null) {
            return DataResult.Error(UiText.DynamicString("QR Code not available"))
        }
        val title = if (content.amount > 0) {
            "${content.shopName} - Pay ₹${content.amount} via UPI"
        } else {
            "${content.shopName} - Scan & Pay UPI QR"
        }
        return upiUseCases.shareQr(matrix, title)
    }

    fun launchWhatsAppPaymentRequest(): DataResult<Unit> {
        val content = _uiState.value as? UpiQrUiState.Content
            ?: return DataResult.Error(UiText.DynamicString("QR Code not generated yet"))
        val upiRequest = UpiRequest(
            upiId = content.upiId,
            payeeName = content.shopName,
            amount = content.amount,
            transactionNote = content.note
        )
        val message = upiUseCases.buildWhatsAppMessage(
            clientName = clientName,
            amount = content.amount,
            services = services,
            request = upiRequest
        )
        return upiWhatsAppLauncher.launchPaymentRequest(phone, message)
    }
}
