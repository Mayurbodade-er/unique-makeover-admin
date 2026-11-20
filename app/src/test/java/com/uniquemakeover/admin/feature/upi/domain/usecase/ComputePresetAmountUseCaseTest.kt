package com.uniquemakeover.admin.feature.upi.domain.usecase

import com.uniquemakeover.admin.feature.upi.domain.model.PaymentPreset
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ComputePresetAmountUseCaseTest {

    private lateinit var useCase: ComputePresetAmountUseCase

    @Before
    fun setUp() {
        useCase = ComputePresetAmountUseCase()
    }

    @Test
    fun advance30_calculatesThirtyPercentWithIntegerRounding() {
        // 355 * 0.30 = 106.5 -> 107
        assertEquals(107, useCase(rate = 355, paid = 0, preset = PaymentPreset.Advance30))

        // 1000 * 0.30 = 300
        assertEquals(300, useCase(rate = 1000, paid = 0, preset = PaymentPreset.Advance30))

        // 1500 * 0.30 = 450
        assertEquals(450, useCase(rate = 1500, paid = 0, preset = PaymentPreset.Advance30))
    }

    @Test
    fun balanceDue_deductsPaidFromTotalRate() {
        // Rate 1500, Paid 500 -> 1000
        assertEquals(1000, useCase(rate = 1500, paid = 500, preset = PaymentPreset.BalanceDue))

        // Rate 1500, Paid 0 -> 1500
        assertEquals(1500, useCase(rate = 1500, paid = 0, preset = PaymentPreset.BalanceDue))

        // Overpaid: Rate 1000, Paid 1200 -> 0 (clamped)
        assertEquals(0, useCase(rate = 1000, paid = 1200, preset = PaymentPreset.BalanceDue))
    }

    @Test
    fun fullPayment_returnsTotalRate() {
        assertEquals(1500, useCase(rate = 1500, paid = 500, preset = PaymentPreset.FullPayment))
        assertEquals(800, useCase(rate = 800, paid = 0, preset = PaymentPreset.FullPayment))
    }

    @Test
    fun customAmount_clampsToStandardBoundaries() {
        // Normal in-range amount
        assertEquals(700, useCase(rate = 1500, paid = 0, preset = PaymentPreset.Custom(700)))

        // Excessive amount clamped to NPCI 1,00,000 ceiling
        assertEquals(100_000, useCase(rate = 1500, paid = 0, preset = PaymentPreset.Custom(500_000)))

        // Zero or negative clamped to minimum 1
        assertEquals(1, useCase(rate = 1500, paid = 0, preset = PaymentPreset.Custom(0)))
        assertEquals(1, useCase(rate = 1500, paid = 0, preset = PaymentPreset.Custom(-50)))
    }
}
