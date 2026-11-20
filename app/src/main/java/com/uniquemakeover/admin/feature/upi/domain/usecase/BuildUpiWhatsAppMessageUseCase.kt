package com.uniquemakeover.admin.feature.upi.domain.usecase

import com.uniquemakeover.admin.feature.upi.domain.model.UpiRequest
import javax.inject.Inject

class BuildUpiWhatsAppMessageUseCase @Inject constructor(
    private val buildUpiUriUseCase: BuildUpiUriUseCase
) {

    operator fun invoke(
        clientName: String,
        amount: Int,
        services: String,
        request: UpiRequest
    ): String {
        val upiUri = buildUpiUriUseCase(request)
        val nameGreeting = if (clientName.isNotBlank()) "Hello $clientName" else "Hello"
        val servicePart = if (services.isNotBlank()) " for your $services booking" else ""

        return """
            $nameGreeting, please pay ₹$amount$servicePart at ${request.payeeName} using this link:
            $upiUri

            Or transfer to UPI ID: ${request.upiId}
            Thank you! ✨
        """.trimIndent()
    }
}
