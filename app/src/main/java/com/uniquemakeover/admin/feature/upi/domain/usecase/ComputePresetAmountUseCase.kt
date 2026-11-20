package com.uniquemakeover.admin.feature.upi.domain.usecase

import com.uniquemakeover.admin.feature.upi.domain.model.PaymentPreset
import javax.inject.Inject
import kotlin.math.roundToInt

class ComputePresetAmountUseCase @Inject constructor() {

    operator fun invoke(rate: Int, paid: Int, preset: PaymentPreset): Int {
        val balanceDue = (rate - paid).coerceAtLeast(0)
        return when (preset) {
            is PaymentPreset.BalanceDue -> balanceDue
            is PaymentPreset.FullPayment -> rate
            is PaymentPreset.Advance30 -> (rate * ADVANCE_PERCENTAGE).roundToInt()
            is PaymentPreset.Custom -> preset.customAmount.coerceIn(MIN_AMOUNT, MAX_AMOUNT)
        }
    }

    companion object {
        private const val ADVANCE_PERCENTAGE = 0.30
        const val MIN_AMOUNT = 1
        const val MAX_AMOUNT = 100_000
    }
}
