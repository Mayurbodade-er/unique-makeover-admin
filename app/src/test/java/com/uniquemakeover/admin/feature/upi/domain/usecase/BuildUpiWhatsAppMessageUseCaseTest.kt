package com.uniquemakeover.admin.feature.upi.domain.usecase

import com.uniquemakeover.admin.feature.upi.domain.model.UpiRequest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BuildUpiWhatsAppMessageUseCaseTest {

    private lateinit var buildUpiUriUseCase: BuildUpiUriUseCase
    private lateinit var buildUpiWhatsAppMessageUseCase: BuildUpiWhatsAppMessageUseCase

    @Before
    fun setUp() {
        buildUpiUriUseCase = BuildUpiUriUseCase()
        buildUpiWhatsAppMessageUseCase = BuildUpiWhatsAppMessageUseCase(buildUpiUriUseCase)
    }

    @Test
    fun invoke_withClientNameAndServices_buildsPersonalizedMessage() {
        val request = UpiRequest(
            upiId = "uniquemakeover@okhdfcbank",
            payeeName = "Unique Makeover",
            amount = 850,
            transactionNote = "Pooja Patil - Salon Bill"
        )

        val message = buildUpiWhatsAppMessageUseCase(
            clientName = "Pooja Patil",
            amount = 850,
            services = "Bridal Hair & Makeup",
            request = request
        )

        assertTrue(message.contains("Hello Pooja Patil"))
        assertTrue(message.contains("please pay ₹850 for your Bridal Hair & Makeup booking"))
        assertTrue(message.contains("at Unique Makeover using this link:"))
        assertTrue(message.contains("upi://pay?pa=uniquemakeover@okhdfcbank"))
        assertTrue(message.contains("Or transfer to UPI ID: uniquemakeover@okhdfcbank"))
        assertTrue(message.contains("Thank you! ✨"))
    }

    @Test
    fun invoke_withoutClientNameAndServices_buildsGenericMessage() {
        val request = UpiRequest(
            upiId = "uniquemakeover@okhdfcbank",
            payeeName = "Unique Makeover",
            amount = 500,
            transactionNote = "Salon Payment"
        )

        val message = buildUpiWhatsAppMessageUseCase(
            clientName = "",
            amount = 500,
            services = "",
            request = request
        )

        assertTrue(message.startsWith("Hello, please pay ₹500 at Unique Makeover using this link:"))
        assertTrue(message.contains("upi://pay?pa=uniquemakeover@okhdfcbank"))
        assertTrue(message.contains("Or transfer to UPI ID: uniquemakeover@okhdfcbank"))
    }
}
