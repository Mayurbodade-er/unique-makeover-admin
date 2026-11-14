package com.uniquemakeover.admin.feature.settings.domain.usecase

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.SlotTimeCalculator
import javax.inject.Inject

class UpdateOperationsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(
        openTime: String,
        closeTime: String,
        slotDurationMinutes: Int,
        rawStaffInput: String
    ): DataResult<Unit> {
        val openMinutes = SlotTimeCalculator.timeToMinutes(openTime)
        val closeMinutes = SlotTimeCalculator.timeToMinutes(closeTime)

        val validationError = validateOperatingHours(openMinutes, closeMinutes, slotDurationMinutes)
        if (validationError != null) {
            return DataResult.Error(validationError)
        }

        val sanitizedStaff = sanitizeStaffInput(rawStaffInput)
        val finalStaffList = sanitizedStaff.ifEmpty { listOf("Sunita") }

        val config = OperationsConfig(
            openTime = openTime,
            closeTime = closeTime,
            slotDurationMinutes = slotDurationMinutes,
            staffList = finalStaffList
        )

        return settingsRepository.updateOperationsConfig(config)
    }

    private fun validateOperatingHours(
        openMinutes: Int,
        closeMinutes: Int,
        slotDurationMinutes: Int
    ): UiText? {
        return when {
            closeMinutes <= openMinutes -> {
                UiText.DynamicString("Closing time must be strictly after opening time")
            }
            slotDurationMinutes <= 0 -> {
                UiText.DynamicString("Slot interval must be greater than zero")
            }
            (closeMinutes - openMinutes) < slotDurationMinutes -> {
                UiText.DynamicString("Operating window must be at least as long as one slot interval")
            }
            else -> null
        }
    }

    private fun sanitizeStaffInput(input: String): List<String> {
        val rawList = input.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val seen = mutableSetOf<String>()
        val result = mutableListOf<String>()
        for (item in rawList) {
            if (seen.add(item.lowercase())) {
                result.add(item)
            }
        }
        return result
    }
}
