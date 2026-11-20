package com.uniquemakeover.admin.feature.settings.presentation

import com.uniquemakeover.admin.core.domain.model.CommunicationConfig
import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.PhoneSanitizer
import com.uniquemakeover.admin.feature.settings.domain.usecase.GetCommunicationConfigUseCase
import com.uniquemakeover.admin.feature.settings.domain.usecase.GetOperationsConfigUseCase
import com.uniquemakeover.admin.feature.settings.domain.usecase.GetReminderTemplateUseCase
import com.uniquemakeover.admin.feature.settings.domain.usecase.UpdateCommunicationUseCase
import com.uniquemakeover.admin.feature.settings.domain.usecase.UpdateOperationsUseCase
import com.uniquemakeover.admin.feature.settings.domain.usecase.UpdateReminderTemplateUseCase
import com.uniquemakeover.admin.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeSettingsRepository
    private lateinit var phoneSanitizer: PhoneSanitizer
    private lateinit var getOperationsConfigUseCase: GetOperationsConfigUseCase
    private lateinit var updateOperationsUseCase: UpdateOperationsUseCase
    private lateinit var getCommunicationConfigUseCase: GetCommunicationConfigUseCase
    private lateinit var updateCommunicationUseCase: UpdateCommunicationUseCase
    private lateinit var getReminderTemplateUseCase: GetReminderTemplateUseCase
    private lateinit var updateReminderTemplateUseCase: UpdateReminderTemplateUseCase
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeSettingsRepository()
        phoneSanitizer = PhoneSanitizer()
        getOperationsConfigUseCase = GetOperationsConfigUseCase(fakeRepository)
        updateOperationsUseCase = UpdateOperationsUseCase(fakeRepository)
        getCommunicationConfigUseCase = GetCommunicationConfigUseCase(fakeRepository)
        updateCommunicationUseCase = UpdateCommunicationUseCase(fakeRepository, phoneSanitizer)
        getReminderTemplateUseCase = GetReminderTemplateUseCase(fakeRepository)
        updateReminderTemplateUseCase = UpdateReminderTemplateUseCase(fakeRepository)
        viewModel = SettingsViewModel(
            getOperationsConfigUseCase = getOperationsConfigUseCase,
            updateOperationsUseCase = updateOperationsUseCase,
            getCommunicationConfigUseCase = getCommunicationConfigUseCase,
            updateCommunicationUseCase = updateCommunicationUseCase,
            getReminderTemplateUseCase = getReminderTemplateUseCase,
            updateReminderTemplateUseCase = updateReminderTemplateUseCase
        )
    }

    @Test
    fun loadOperationsConfig_updatesStateWithConfig() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("09:00", state.operationsConfig.openTime)
        assertEquals("20:00", state.operationsConfig.closeTime)
        assertEquals(30, state.operationsConfig.slotDurationMinutes)
        assertEquals(listOf("Sunita", "Pooja"), state.operationsConfig.staffList)
        assertEquals("9822001122", state.communicationConfig.whatsappNumber)
        assertEquals("uniquemakeover@upi", state.communicationConfig.merchantUpiId)
        assertEquals("Hello {name}, your appointment at {shop} is on {date}.", state.reminderTemplate)
    }

    @Test
    fun saveOperations_validInput_updatesConfigAndSetsSuccessMessage() = runTest {
        advanceUntilIdle()
        viewModel.saveOperations("10:00", "19:00", 45, "Sunita, Anjali")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertNotNull(state.successMessage)
        assertEquals("10:00", state.operationsConfig.openTime)
        assertEquals("19:00", state.operationsConfig.closeTime)
        assertEquals(45, state.operationsConfig.slotDurationMinutes)
        assertEquals(listOf("Sunita", "Anjali"), state.operationsConfig.staffList)
    }

    @Test
    fun saveOperations_invalidInput_setsErrorMessage() = runTest {
        advanceUntilIdle()
        viewModel.saveOperations("19:00", "10:00", 30, "Sunita")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertNull(state.successMessage)
    }

    @Test
    fun saveCommunication_validInput_updatesStateAndSetsSuccessMessage() = runTest {
        advanceUntilIdle()
        viewModel.saveCommunication("9890123456", "salon@okaxis")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.commsErrorMessage)
        assertNotNull(state.successMessage)
        assertEquals("9890123456", state.communicationConfig.whatsappNumber)
        assertEquals("salon@okaxis", state.communicationConfig.merchantUpiId)
    }

    @Test
    fun saveCommunication_invalidPhone_setsErrorMessage() = runTest {
        advanceUntilIdle()
        viewModel.saveCommunication("123", "salon@okaxis")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.commsErrorMessage)
        assertNull(state.successMessage)
    }

    @Test
    fun saveReminderTemplate_validInput_updatesStateAndSetsSuccessMessage() = runTest {
        advanceUntilIdle()
        viewModel.saveReminderTemplate("Hello {name}, your appointment at {shop} is confirmed!")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.templateErrorMessage)
        assertNotNull(state.successMessage)
        assertEquals("Hello {name}, your appointment at {shop} is confirmed!", state.reminderTemplate)
    }

    @Test
    fun saveReminderTemplate_emptyInput_setsErrorMessage() = runTest {
        advanceUntilIdle()
        viewModel.saveReminderTemplate("   ")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.templateErrorMessage)
        assertNull(state.successMessage)
    }

    @Test
    fun clearMessages_clearsBothSuccessAndError() = runTest {
        advanceUntilIdle()
        viewModel.saveOperations("19:00", "10:00", 30, "Sunita")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearMessages()
        assertNull(viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.commsErrorMessage)
        assertNull(viewModel.uiState.value.templateErrorMessage)
        assertNull(viewModel.uiState.value.successMessage)
    }

    private class FakeSettingsRepository : SettingsRepository {
        var config = OperationsConfig(
            openTime = "09:00",
            closeTime = "20:00",
            slotDurationMinutes = 30,
            staffList = listOf("Sunita", "Pooja")
        )
        var commsConfig = CommunicationConfig(
            whatsappNumber = "9822001122",
            merchantUpiId = "uniquemakeover@upi"
        )

        override fun getSettings(): Flow<Settings?> = flowOf(null)
        override suspend fun getSettingsSync(): Settings? = null
        override suspend fun getOperationsConfig(): OperationsConfig = config
        override suspend fun updateOperationsConfig(newConfig: OperationsConfig): DataResult<Unit> {
            config = newConfig
            return DataResult.Success(Unit)
        }
        override suspend fun updateSettings(settings: Settings): DataResult<Unit> = DataResult.Success(Unit)

        override suspend fun getCommunicationConfig(): CommunicationConfig = commsConfig
        override suspend fun updateCommunicationConfig(config: CommunicationConfig): DataResult<Unit> {
            commsConfig = config
            return DataResult.Success(Unit)
        }

        var reminderTemplate: String = "Hello {name}, your appointment at {shop} is on {date}."
        override suspend fun getReminderTemplate(): String = reminderTemplate
        override suspend fun updateReminderTemplate(template: String): DataResult<Unit> {
            reminderTemplate = template
            return DataResult.Success(Unit)
        }
    }
}
