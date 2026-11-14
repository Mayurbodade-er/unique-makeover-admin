package com.uniquemakeover.admin.feature.appointments.presentation

import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.core.domain.model.Slot
import com.uniquemakeover.admin.core.domain.model.UnavailableReason
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.repository.SlotRepository
import com.uniquemakeover.admin.feature.appointments.domain.usecase.GenerateAvailableSlotsUseCase
import com.uniquemakeover.admin.testutil.FakeTimeProvider
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
class AppointmentsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeSettingsRepository: FakeSettingsRepository
    private lateinit var fakeSlotRepository: FakeSlotRepository
    private lateinit var useCase: GenerateAvailableSlotsUseCase
    private lateinit var viewModel: AppointmentsViewModel

    @Before
    fun setUp() {
        fakeTimeProvider = FakeTimeProvider("10:00", "2026-11-20")
        fakeSettingsRepository = FakeSettingsRepository()
        fakeSlotRepository = FakeSlotRepository()
        useCase = GenerateAvailableSlotsUseCase(
            settingsRepository = fakeSettingsRepository,
            slotRepository = fakeSlotRepository,
            timeProvider = fakeTimeProvider
        )
        viewModel = AppointmentsViewModel(useCase, fakeTimeProvider)
    }

    @Test
    fun init_loadsSlotsForToday() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals("2026-11-20", state.selectedDate)
        assertEquals(6, state.slots.size)
        assertNull(state.selectedSlot)
    }

    @Test
    fun selectSlot_availableSlot_updatesSelectedSlot() = runTest {
        advanceUntilIdle()
        val openSlot = viewModel.uiState.value.slots.first { it.isAvailable }

        viewModel.selectSlot(openSlot)
        assertEquals(openSlot.time24, viewModel.uiState.value.selectedSlot?.time24)
    }

    @Test
    fun selectSlot_unavailableSlot_doesNotSelect() = runTest {
        advanceUntilIdle()
        val unavailableSlot = Slot("12:00", "12:00 PM", isAvailable = false, reason = UnavailableReason.ALREADY_BOOKED)

        viewModel.selectSlot(unavailableSlot)
        assertNull(viewModel.uiState.value.selectedSlot)
    }

    @Test
    fun selectDate_updatesDateAndReloadsSlots() = runTest {
        advanceUntilIdle()
        viewModel.selectDate("2026-11-25")
        advanceUntilIdle()

        assertEquals("2026-11-25", viewModel.uiState.value.selectedDate)
        assertEquals(6, viewModel.uiState.value.slots.size)
    }

    private class FakeSettingsRepository : SettingsRepository {
        var config = OperationsConfig(
            openTime = "10:00",
            closeTime = "13:00",
            slotDurationMinutes = 30,
            staffList = listOf("Sunita")
        )

        override fun getSettings(): Flow<Settings?> = flowOf(null)
        override suspend fun getSettingsSync(): Settings? = null
        override suspend fun getOperationsConfig(): OperationsConfig = config
        override suspend fun updateOperationsConfig(newConfig: OperationsConfig): DataResult<Unit> =
            DataResult.Success(Unit)
        override suspend fun updateSettings(settings: Settings): DataResult<Unit> =
            DataResult.Success(Unit)
    }

    private class FakeSlotRepository : SlotRepository {
        override suspend fun isClashFree(date: String, time: String, excludeId: String?): Boolean = true
        override suspend fun getAvailableSlots(date: String): List<Slot> = emptyList()
    }
}
