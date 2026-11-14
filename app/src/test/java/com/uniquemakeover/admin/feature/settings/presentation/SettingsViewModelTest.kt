package com.uniquemakeover.admin.feature.settings.presentation

import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.settings.domain.usecase.GetOperationsConfigUseCase
import com.uniquemakeover.admin.feature.settings.domain.usecase.UpdateOperationsUseCase
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
    private lateinit var getOperationsConfigUseCase: GetOperationsConfigUseCase
    private lateinit var updateOperationsUseCase: UpdateOperationsUseCase
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeSettingsRepository()
        getOperationsConfigUseCase = GetOperationsConfigUseCase(fakeRepository)
        updateOperationsUseCase = UpdateOperationsUseCase(fakeRepository)
        viewModel = SettingsViewModel(getOperationsConfigUseCase, updateOperationsUseCase)
    }

    @Test
    fun loadOperationsConfig_updatesStateWithConfig() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("09:00", state.operationsConfig.openTime)
        assertEquals("20:00", state.operationsConfig.closeTime)
        assertEquals(30, state.operationsConfig.slotDurationMinutes)
        assertEquals(listOf("Sunita", "Pooja"), state.operationsConfig.staffList)
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
    fun clearMessages_clearsBothSuccessAndError() = runTest {
        advanceUntilIdle()
        viewModel.saveOperations("19:00", "10:00", 30, "Sunita")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearMessages()
        assertNull(viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.successMessage)
    }

    private class FakeSettingsRepository : SettingsRepository {
        var config = OperationsConfig(
            openTime = "09:00",
            closeTime = "20:00",
            slotDurationMinutes = 30,
            staffList = listOf("Sunita", "Pooja")
        )

        override fun getSettings(): Flow<Settings?> = flowOf(null)
        override suspend fun getSettingsSync(): Settings? = null
        override suspend fun getOperationsConfig(): OperationsConfig = config
        override suspend fun updateOperationsConfig(newConfig: OperationsConfig): DataResult<Unit> {
            config = newConfig
            return DataResult.Success(Unit)
        }
        override suspend fun updateSettings(settings: Settings): DataResult<Unit> = DataResult.Success(Unit)
    }
}
