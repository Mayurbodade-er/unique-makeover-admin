package com.uniquemakeover.admin.feature.appointments.domain.usecase

import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.util.PhoneSanitizer
import com.uniquemakeover.admin.feature.appointments.domain.model.BookingDraft
import com.uniquemakeover.admin.feature.appointments.domain.model.BookingValidationResult
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ValidateBookingDraftUseCaseTest {

    private lateinit var sanitizer: PhoneSanitizer
    private lateinit var useCase: ValidateBookingDraftUseCase

    private val sampleService = Service("s1", "Gold Facial", 800)

    @Before
    fun setUp() {
        sanitizer = PhoneSanitizer()
        useCase = ValidateBookingDraftUseCase(sanitizer)
    }

    @Test
    fun invoke_validDraft_returnsValid() {
        val draft = BookingDraft(
            name = "Pooja Patil",
            phone = "9822001122",
            selectedServices = listOf(sampleService),
            date = "2026-11-20",
            time = "10:00",
            staff = "Sunita",
            discount = 100,
            advancePaid = 200
        )

        val result = useCase(draft)
        assertTrue(result is BookingValidationResult.Valid)
    }

    @Test
    fun invoke_blankName_returnsNameError() {
        val draft = BookingDraft(
            name = "   ",
            phone = "9822001122",
            selectedServices = listOf(sampleService),
            date = "2026-11-20",
            time = "10:00"
        )

        val result = useCase(draft) as BookingValidationResult.Invalid
        assertNotNull(result.nameError)
    }

    @Test
    fun invoke_invalidPhone_returnsPhoneError() {
        val invalidPhones = listOf("12345", "abcdefghij", "0000000000", "1234567890")

        invalidPhones.forEach { phone ->
            val draft = BookingDraft(
                name = "Pooja Patil",
                phone = phone,
                selectedServices = listOf(sampleService),
                date = "2026-11-20",
                time = "10:00"
            )

            val result = useCase(draft) as BookingValidationResult.Invalid
            assertNotNull("Phone $phone should be rejected", result.phoneError)
        }
    }

    @Test
    fun invoke_emptyServices_returnsServiceError() {
        val draft = BookingDraft(
            name = "Pooja Patil",
            phone = "9822001122",
            selectedServices = emptyList(),
            date = "2026-11-20",
            time = "10:00"
        )

        val result = useCase(draft) as BookingValidationResult.Invalid
        assertNotNull(result.serviceError)
    }

    @Test
    fun invoke_discountExceedsSubtotal_returnsFinancialError() {
        val draft = BookingDraft(
            name = "Pooja Patil",
            phone = "9822001122",
            selectedServices = listOf(sampleService), // 800
            date = "2026-11-20",
            time = "10:00",
            discount = 1000
        )

        val result = useCase(draft) as BookingValidationResult.Invalid
        assertNotNull(result.financialError)
    }

    @Test
    fun invoke_advanceExceedsFinalRate_returnsFinancialError() {
        val draft = BookingDraft(
            name = "Pooja Patil",
            phone = "9822001122",
            selectedServices = listOf(sampleService), // 800
            date = "2026-11-20",
            time = "10:00",
            discount = 200, // final rate = 600
            advancePaid = 700
        )

        val result = useCase(draft) as BookingValidationResult.Invalid
        assertNotNull(result.financialError)
    }
}
