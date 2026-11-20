package com.uniquemakeover.admin.feature.upi.domain.usecase

import com.uniquemakeover.admin.feature.upi.domain.model.UpiRequest
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import javax.inject.Inject

class BuildUpiUriUseCase @Inject constructor() {

    operator fun invoke(request: UpiRequest): String {
        val encodedPayee = encodeParam(request.payeeName)
        val encodedNote = encodeParam(request.transactionNote)
        val amountPart = if (request.amount > 0) {
            val formattedAmount = String.format(Locale.US, "%.2f", request.amount.toDouble())
            "&am=$formattedAmount"
        } else {
            ""
        }
        return "upi://pay?pa=${request.upiId}&pn=$encodedPayee$amountPart&cu=INR&tn=$encodedNote"
    }

    private fun encodeParam(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")
    }
}
