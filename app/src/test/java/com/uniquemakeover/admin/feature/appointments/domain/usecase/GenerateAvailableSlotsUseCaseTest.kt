package com.uniquemakeover.admin.feature.appointments.domain.usecase

import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.core.domain.model.Slot
import com.uniquemakeover.admin.core.domain.model.UnavailableReason
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.appointments.domain.repository.SlotRepository
import com.uniquemakeover.admin.testutil.FakeTimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GenerateAvailableSlotsUseCaseTest {

    private lateinit var fakeSettingsRepository: FakeSettingsRepository
    private lateinit var fakeSlotRepository: FakeSlotRepository
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var useCase: GenerateAvailableSlotsUseCase

    @Before
    fun setUp() {
        fakeSettingsRepository = FakeSettingsRepository()
        fakeSlotRepository = FakeSlotRepository()
        fakeTimeProvider = FakeTimeProvider(
            currentTime24 = "10:00",
            currentDate = "2026-11-20"
        )
        useCase = GenerateAvailableSlotsUseCase(
            settingsRepository = fakeSettingsRepository,
            slotRepository = fakeSlotRepository,
            timeProvider = fakeTimeProvider
        )
    }

    @Test
    fun invoke_futureDate_generatesExactSlotCountAndFormatting() = runTest {
        fakeSettingsRepository.config = OperationsConfig(
            openTime = "10:00",
            closeTime = "13:00",
            slotDurationMinutes = 30,
            staffList = listOf("Sunita")
        )

        val slots = useCase("2026-11-21")

        assertEquals(6, slots.size)
        assertEquals("10:00", slots[0].time24)
        assertEquals("10:00 AM", slots[0].time12)
        assertEquals("12:30", slots[5].time24)
        assertEquals("12:30 PM", slots[5].time12)
        assertTrue(slots.all { it.isAvailable && it.reason == null })
    }

    @Test
    fun invoke_clashingSlot_marksAlreadyBooked() = runTest {
        fakeSettingsRepository.config = OperationsConfig(
            openTime = "10:00",
            closeTime = "13:00",
            slotDurationMinutes = 30,
            staffList = listOf("Sunita")
        )
        fakeSlotRepository.bookedTimes.add("2026-11-21_11:30")

        val slots = useCase("2026-11-21")

        val clashingSlot = slots.first { it.time24 == "11:30" }
        assertFalse(clashingSlot.isAvailable)
        assertEquals(UnavailableReason.ALREADY_BOOKED, clashingSlot.reason)

        val openSlot = slots.first { it.time24 == "10:00" }
        assertTrue(openSlot.isAvailable)
        assertNull(openSlot.reason)
    }

    @Test
    fun invoke_today_marksPastSlotsAsUnavailable() = runTest {
        fakeTimeProvider.currentDate = "2026-11-20"
        fakeTimeProvider.currentTime24 = "14:15"

        fakeSettingsRepository.config = OperationsConfig(
            openTime = "10:00",
            closeTime = "18:00",
            slotDurationMinutes = 30,
            staffList = listOf("Sunita")
        )

        val slots = useCase("2026-11-20")

        val slot1400 = slots.first { it.time24 == "14:00" }
        assertFalse(slot1400.isAvailable)
        assertEquals(UnavailableReason.PAST_TIME, slot1400.reason)

        val slot1430 = slots.first { it.time24 == "14:30" }
        assertTrue(slot1430.isAvailable)
        assertNull(slot1430.reason)
    }

    @Test
    fun invoke_lastSlotBoundary_neverExceedsCloseTimeMinusSlotDuration() = runTest {
        fakeSettingsRepository.config = OperationsConfig(
            openTime = "09:00",
            closeTime = "20:00",
            slotDurationMinutes = 30,
            staffList = listOf("Sunita")
        )

        val slots = useCase("2026-11-25")

        assertEquals("09:00", slots.first().time24)
        assertEquals("19:30", slots.last().time24)
        assertFalse(slots.any { it.time24 == "20:00" })
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
        val bookedTimes = mutableSetOf<String>()

        override suspend fun isClashFree(date: String, time: String, excludeId: String?): Boolean {
            return !bookedTimes.contains("${date}_$time")
        }

        override suspend fun getAvailableSlots(date: String): List<Slot> = emptyList()
    }
}
