package com.uniquemakeover.admin.feature.appointments.domain.usecase

import com.uniquemakeover.admin.core.domain.model.Slot
import com.uniquemakeover.admin.core.domain.model.UnavailableReason
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.util.SlotTimeCalculator
import com.uniquemakeover.admin.core.util.TimeProvider
import com.uniquemakeover.admin.feature.appointments.domain.repository.SlotRepository
import javax.inject.Inject

private const val DEFAULT_SLOT_MINUTES = 30

class GenerateAvailableSlotsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val slotRepository: SlotRepository,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(
        selectedDate: String,
        excludeAppointmentId: String? = null,
        existingSlotTime: String? = null
    ): List<Slot> {
        val config = settingsRepository.getOperationsConfig()
        val openMinutes = SlotTimeCalculator.timeToMinutes(config.openTime)
        val closeMinutes = SlotTimeCalculator.timeToMinutes(config.closeTime)
        val step = if (config.slotDurationMinutes > 0) config.slotDurationMinutes else DEFAULT_SLOT_MINUTES

        val slots = mutableListOf<Slot>()
        var current = openMinutes

        while (current + step <= closeMinutes) {
            val time24 = SlotTimeCalculator.minutesToTime(current)
            val time12 = SlotTimeCalculator.format12Hour(time24)
            val reason = determineSlotUnavailability(selectedDate, current, time24, excludeAppointmentId)

            slots.add(
                Slot(
                    time24 = time24,
                    time12 = time12,
                    isAvailable = reason == null,
                    reason = reason
                )
            )
            current += step
        }

        if (!existingSlotTime.isNullOrBlank() && slots.none { it.time24 == existingSlotTime }) {
            val existingMinutes = SlotTimeCalculator.timeToMinutes(existingSlotTime)
            val existingSlot = Slot(
                time24 = existingSlotTime,
                time12 = SlotTimeCalculator.format12Hour(existingSlotTime),
                isAvailable = true,
                reason = null
            )
            val insertIndex = slots.indexOfFirst { SlotTimeCalculator.timeToMinutes(it.time24) > existingMinutes }
            if (insertIndex >= 0) {
                slots.add(insertIndex, existingSlot)
            } else {
                slots.add(existingSlot)
            }
        }

        return slots
    }

    private suspend fun determineSlotUnavailability(
        selectedDate: String,
        slotMinutes: Int,
        time24: String,
        excludeAppointmentId: String?
    ): UnavailableReason? {
        val today = timeProvider.todayDate()
        val isPastDate = selectedDate < today
        val isToday = selectedDate == today
        val nowMinutes = SlotTimeCalculator.timeToMinutes(timeProvider.nowTime24())
        val isPastTime = isPastDate || (isToday && slotMinutes < nowMinutes)

        return when {
            isPastTime -> UnavailableReason.PAST_TIME
            !slotRepository.isClashFree(selectedDate, time24, excludeAppointmentId) -> UnavailableReason.ALREADY_BOOKED
            else -> null
        }
    }
}
