package com.uniquemakeover.admin.feature.settings.domain.usecase

import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.domain.model.CommunicationConfig
import com.uniquemakeover.admin.core.domain.repository.SettingsRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.core.util.PhoneSanitizer
import com.uniquemakeover.admin.core.util.UpiValidator
import javax.inject.Inject

class UpdateCommunicationUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val phoneSanitizer: PhoneSanitizer
) {
    suspend operator fun invoke(rawPhone: String, rawUpiId: String): DataResult<Unit> {
        val trimmedPhone = rawPhone.trim()
        val trimmedUpi = rawUpiId.trim()

        val sanitizedPhone = phoneSanitizer.sanitize(trimmedPhone)
        val validationError = when {
            sanitizedPhone == null -> UiText.DynamicString("Please enter a valid 10-digit WhatsApp mobile number")
            trimmedUpi.isNotEmpty() && !UpiValidator.isValid(trimmedUpi) -> {
                UiText.DynamicString("Please enter a valid UPI ID (e.g. name@bank)")
            }
            else -> null
        }

        if (validationError != null) {
            return DataResult.Error(validationError)
        }

        val config = CommunicationConfig(
            whatsappNumber = requireNotNull(sanitizedPhone),
            merchantUpiId = trimmedUpi
        )
        return settingsRepository.updateCommunicationConfig(config)
    }
}
