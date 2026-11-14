package com.uniquemakeover.admin.feature.settings.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class UpdateOperationsUseCaseTest {

    private val repository = mockk<SettingsRepository>()
    private lateinit var useCase: UpdateOperationsUseCase

    @Before
    fun setUp() {
        useCase = UpdateOperationsUseCase(repository)
    }

    @Test
    fun invoke_closeTimeBeforeOrEqualOpenTime_returnsError() = runBlocking {
        // Equal times
        val resultEqual = useCase(
            openTime = "10:00",
            closeTime = "10:00",
            slotDurationMinutes = 30,
            rawStaffInput = "Sunita"
        )
        assertThat(resultEqual).isInstanceOf(DataResult.Error::class.java)
        val errorMsgEqual = ((resultEqual as DataResult.Error).message as UiText.DynamicString).value
        assertThat(errorMsgEqual).contains("Closing time must be strictly after opening time")

        // Close before open
        val resultBefore = useCase(
            openTime = "10:00",
            closeTime = "09:00",
            slotDurationMinutes = 30,
            rawStaffInput = "Sunita"
        )
        assertThat(resultBefore).isInstanceOf(DataResult.Error::class.java)
        coVerify(exactly = 0) { repository.updateOperationsConfig(any()) }
    }

    @Test
    fun invoke_slotDurationLessThanOrEqualToZero_returnsError() = runBlocking {
        val result = useCase(
            openTime = "10:00",
            closeTime = "18:00",
            slotDurationMinutes = 0,
            rawStaffInput = "Sunita"
        )
        assertThat(result).isInstanceOf(DataResult.Error::class.java)
        val errorMsg = ((result as DataResult.Error).message as UiText.DynamicString).value
        assertThat(errorMsg).contains("Slot interval must be greater than zero")
        coVerify(exactly = 0) { repository.updateOperationsConfig(any()) }
    }

    @Test
    fun invoke_slotDurationExceedsOperatingWindow_returnsError() = runBlocking {
        // Window is 45 mins (10:00 to 10:45) but slot is 60 mins (EC-SET-02.1)
        val result = useCase(
            openTime = "10:00",
            closeTime = "10:45",
            slotDurationMinutes = 60,
            rawStaffInput = "Sunita"
        )
        assertThat(result).isInstanceOf(DataResult.Error::class.java)
        val errorMsg = ((result as DataResult.Error).message as UiText.DynamicString).value
        assertThat(errorMsg).contains("Operating window must be at least as long as one slot interval")
        coVerify(exactly = 0) { repository.updateOperationsConfig(any()) }
    }

    @Test
    fun invoke_sanitizesAndDeduplicatesStaffNames() = runBlocking {
        val configSlot = slot<OperationsConfig>()
        coEvery { repository.updateOperationsConfig(capture(configSlot)) } returns DataResult.Success(Unit)

        val result = useCase(
            openTime = "09:30",
            closeTime = "20:30",
            slotDurationMinutes = 30,
            rawStaffInput = "Sunita, Pooja, , sunita, Priya,  "
        )

        assertThat(result).isInstanceOf(DataResult.Success::class.java)
        assertThat(configSlot.captured.staffList).containsExactly("Sunita", "Pooja", "Priya").inOrder()
        assertThat(configSlot.captured.openTime).isEqualTo("09:30")
        assertThat(configSlot.captured.closeTime).isEqualTo("20:30")
        assertThat(configSlot.captured.slotDurationMinutes).isEqualTo(30)
    }
}
