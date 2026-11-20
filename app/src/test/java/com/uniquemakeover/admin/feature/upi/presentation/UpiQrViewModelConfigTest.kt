package com.uniquemakeover.admin.feature.upi.presentation

import androidx.lifecycle.SavedStateHandle
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.CommunicationConfig
import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.upi.domain.launcher.UpiWhatsAppLauncher
import com.uniquemakeover.admin.feature.upi.domain.model.PaymentPreset
import com.uniquemakeover.admin.feature.upi.domain.model.QrCodeMatrix
import com.uniquemakeover.admin.feature.upi.domain.repository.QrImageExporter
import com.uniquemakeover.admin.feature.upi.domain.repository.QrRepository
import com.uniquemakeover.admin.feature.upi.domain.usecase.BuildUpiUriUseCase
import com.uniquemakeover.admin.feature.upi.domain.usecase.BuildUpiWhatsAppMessageUseCase
import com.uniquemakeover.admin.feature.upi.domain.usecase.ComputePresetAmountUseCase
import com.uniquemakeover.admin.feature.upi.domain.usecase.GenerateQrMatrixUseCase
import com.uniquemakeover.admin.feature.upi.domain.usecase.SaveQrPngUseCase
import com.uniquemakeover.admin.feature.upi.domain.usecase.ShareQrUseCase
import com.uniquemakeover.admin.feature.upi.domain.usecase.UpiUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class UpiQrViewModelConfigTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeSettingsRepository: FakeSettingsRepository
    private lateinit var fakeWhatsAppLauncher: FakeUpiWhatsAppLauncher
    private lateinit var upiUseCases: UpiUseCases

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeSettingsRepository = FakeSettingsRepository()
        fakeWhatsAppLauncher = FakeUpiWhatsAppLauncher()
        val buildUpiUriUseCase = BuildUpiUriUseCase()
        val generateQrMatrixUseCase = GenerateQrMatrixUseCase(FakeQrRepository())
        val computePresetAmountUseCase = ComputePresetAmountUseCase()
        val buildWhatsAppMessageUseCase = BuildUpiWhatsAppMessageUseCase(buildUpiUriUseCase)
        val fakeQrImageExporter = FakeQrImageExporter()
        val saveQrPngUseCase = SaveQrPngUseCase(fakeQrImageExporter)
        val shareQrUseCase = ShareQrUseCase(fakeQrImageExporter)

        upiUseCases = UpiUseCases(
            buildUpiUri = buildUpiUriUseCase,
            generateQrMatrix = generateQrMatrixUseCase,
            computePresetAmount = computePresetAmountUseCase,
            buildWhatsAppMessage = buildWhatsAppMessageUseCase,
            saveQrPng = saveQrPngUseCase,
            shareQr = shareQrUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ): UpiQrViewModel {
        return UpiQrViewModel(
            settingsRepository = fakeSettingsRepository,
            upiUseCases = upiUseCases,
            upiWhatsAppLauncher = fakeWhatsAppLauncher,
            savedStateHandle = savedStateHandle
        )
    }

    @Test
    fun nullSettings_emitsMissingConfiguration() = runTest {
        fakeSettingsRepository.emitSettings(null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(UpiQrUiState.MissingConfiguration, viewModel.uiState.value)
    }

    @Test
    fun emptyUpi_emitsMissingConfiguration() = runTest {
        fakeSettingsRepository.emitSettings(Settings(shop = "Unique Makeover", upi = ""))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(UpiQrUiState.MissingConfiguration, viewModel.uiState.value)
    }

    @Test
    fun whitespaceUpi_emitsMissingConfiguration() = runTest {
        fakeSettingsRepository.emitSettings(Settings(shop = "Unique Makeover", upi = "   "))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(UpiQrUiState.MissingConfiguration, viewModel.uiState.value)
    }

    @Test
    fun invalidHandleUpi_emitsMissingConfiguration() = runTest {
        fakeSettingsRepository.emitSettings(Settings(shop = "Unique Makeover", upi = "invalid-handle-no-at"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(UpiQrUiState.MissingConfiguration, viewModel.uiState.value)
    }

    @Test
    fun validUpi_emitsContentWithDetailsAndQrBitmap() = runTest {
        fakeSettingsRepository.emitSettings(
            Settings(shop = "Unique Salon", upi = "uniquemakeover@okhdfcbank")
        )

        val savedStateHandle = SavedStateHandle(
            mapOf("amount" to 850, "clientName" to "Pooja Patil")
        )

        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Content state but was $state", state is UpiQrUiState.Content)
        val content = state as UpiQrUiState.Content
        assertEquals("Unique Salon", content.shopName)
        assertEquals("uniquemakeover@okhdfcbank", content.upiId)
        assertEquals(850, content.amount)
        assertEquals("Pooja Patil - Salon Bill", content.note)
        assertNotNull("QR Bitmap must be generated", content.qrBitmap)
        assertTrue(content.upiUri.contains("uniquemakeover@okhdfcbank"))
    }

    @Test
    fun onPresetSelected_advance30_updatesAmountAndRegenerates() = runTest {
        fakeSettingsRepository.emitSettings(
            Settings(shop = "Unique Salon", upi = "uniquemakeover@okhdfcbank")
        )

        val savedStateHandle = SavedStateHandle(
            mapOf("rate" to 1500, "paid" to 0, "clientName" to "Rani")
        )

        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        // Default is BalanceDue -> 1500
        val initialContent = viewModel.uiState.value as UpiQrUiState.Content
        assertEquals(1500, initialContent.amount)

        // Select Advance (30%) -> 450
        viewModel.onPresetSelected(PaymentPreset.Advance30)
        advanceUntilIdle()

        val updatedContent = viewModel.uiState.value as UpiQrUiState.Content
        assertEquals(450, updatedContent.amount)
        assertEquals(PaymentPreset.Advance30, updatedContent.selectedPreset)
        assertTrue(updatedContent.upiUri.contains("am=450.00"))
    }

    @Test
    fun onCustomAmountChanged_updatesAmountAndRegenerates() = runTest {
        fakeSettingsRepository.emitSettings(
            Settings(shop = "Unique Salon", upi = "uniquemakeover@okhdfcbank")
        )

        val savedStateHandle = SavedStateHandle(
            mapOf("rate" to 1000, "paid" to 0, "clientName" to "Neha")
        )

        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        viewModel.onCustomAmountChanged("650")
        advanceUntilIdle()

        val updatedContent = viewModel.uiState.value as UpiQrUiState.Content
        assertEquals(650, updatedContent.amount)
        assertEquals(PaymentPreset.Custom(650), updatedContent.selectedPreset)
        assertEquals("650", updatedContent.customAmountInput)
        assertTrue(updatedContent.upiUri.contains("am=650.00"))
    }

    @Test
    fun launchWhatsAppPaymentRequest_whenContentReady_dispatchesFormattedMessage() = runTest {
        fakeSettingsRepository.emitSettings(
            Settings(shop = "Unique Salon", upi = "uniquemakeover@okhdfcbank")
        )

        val savedStateHandle = SavedStateHandle(
            mapOf(
                "amount" to 850,
                "clientName" to "Pooja Patil",
                "phone" to "9876543210",
                "services" to "Facial"
            )
        )

        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        val result = viewModel.launchWhatsAppPaymentRequest()
        assertTrue(result is DataResult.Success)
        assertEquals("9876543210", fakeWhatsAppLauncher.lastPhone)
        val message = fakeWhatsAppLauncher.lastMessage ?: ""
        assertTrue(message.contains("Hello Pooja Patil"))
        assertTrue(message.contains("₹850 for your Facial booking"))
        assertTrue(message.contains("upi://pay?pa=uniquemakeover@okhdfcbank"))
    }

    @Test
    fun launchWhatsAppPaymentRequest_whenNotContent_returnsError() = runTest {
        fakeSettingsRepository.emitSettings(null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val result = viewModel.launchWhatsAppPaymentRequest()
        assertTrue(result is DataResult.Error)
    }

    @Test
    fun saveToGallery_whenContentReady_savesImageSuccessfully() = runTest {
        fakeSettingsRepository.emitSettings(
            Settings(shop = "Unique Salon", upi = "uniquemakeover@okhdfcbank")
        )
        val savedStateHandle = SavedStateHandle(mapOf("amount" to 500))
        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        val result = viewModel.saveToGallery()
        assertTrue(result is DataResult.Success)
    }

    @Test
    fun saveToGallery_whenNotContent_returnsError() = runTest {
        fakeSettingsRepository.emitSettings(null)
        val viewModel = createViewModel()
        advanceUntilIdle()

        val result = viewModel.saveToGallery()
        assertTrue(result is DataResult.Error)
    }

    @Test
    fun shareQr_whenContentReady_sharesSuccessfully() = runTest {
        fakeSettingsRepository.emitSettings(
            Settings(shop = "Unique Salon", upi = "uniquemakeover@okhdfcbank")
        )
        val savedStateHandle = SavedStateHandle(mapOf("amount" to 500))
        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        val result = viewModel.shareQr()
        assertTrue(result is DataResult.Success)
    }

    @Test
    fun shareQr_whenNotContent_returnsError() = runTest {
        fakeSettingsRepository.emitSettings(null)
        val viewModel = createViewModel()
        advanceUntilIdle()

        val result = viewModel.shareQr()
        assertTrue(result is DataResult.Error)
    }

    private class FakeQrImageExporter : QrImageExporter {
        override suspend fun saveToGallery(
            matrix: QrCodeMatrix,
            filename: String
        ): DataResult<String> = DataResult.Success("content://media/external/images/media/1")

        override suspend fun shareQr(matrix: QrCodeMatrix, title: String): DataResult<Unit> =
            DataResult.Success(Unit)
    }

    private class FakeUpiWhatsAppLauncher : UpiWhatsAppLauncher {
        var lastPhone: String? = null
        var lastMessage: String? = null
        var shouldFail: Boolean = false

        override fun launchPaymentRequest(phone: String, message: String): DataResult<Unit> {
            lastPhone = phone
            lastMessage = message
            return if (shouldFail) {
                DataResult.Error(UiText.DynamicString("WhatsApp not installed"))
            } else {
                DataResult.Success(Unit)
            }
        }
    }

    private class FakeQrRepository : QrRepository {
        override suspend fun generateQrMatrix(payload: String, sizePx: Int): QrCodeMatrix =
            QrCodeMatrix(sizePx, sizePx, BooleanArray(sizePx * sizePx))
    }

    private class FakeSettingsRepository : SettingsRepository {
        private val settingsFlow = MutableStateFlow<Settings?>(null)

        fun emitSettings(settings: Settings?) {
            settingsFlow.value = settings
        }

        override fun getSettings(): Flow<Settings?> = settingsFlow

        override suspend fun getSettingsSync(): Settings? = settingsFlow.first()

        override suspend fun getOperationsConfig(): OperationsConfig = OperationsConfig()

        override suspend fun updateOperationsConfig(config: OperationsConfig): DataResult<Unit> =
            DataResult.Success(Unit)

        override suspend fun updateSettings(settings: Settings): DataResult<Unit> =
            DataResult.Success(Unit)

        override suspend fun getCommunicationConfig(): CommunicationConfig = CommunicationConfig()

        override suspend fun updateCommunicationConfig(config: CommunicationConfig): DataResult<Unit> =
            DataResult.Success(Unit)

        override suspend fun getReminderTemplate(): String = ""

        override suspend fun updateReminderTemplate(template: String): DataResult<Unit> =
            DataResult.Success(Unit)
    }
}
