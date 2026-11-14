package com.uniquemakeover.admin.core.util

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneSanitizer @Inject constructor() {

    private val indianMobileRegex = Regex("^[6-9]\\d{9}$")
    private val nonDigitRegex = Regex("[^0-9]")

    companion object {
        private const val WITH_COUNTRY_CODE_LENGTH = 12
        private const val WITH_LEADING_ZERO_LENGTH = 11
        private const val STANDARD_MOBILE_LENGTH = 10
        private const val COUNTRY_CODE_PREFIX_LENGTH = 2
        private const val LEADING_ZERO_PREFIX_LENGTH = 1
    }

    fun sanitize(rawPhone: String): String? {
        val digitsOnly = rawPhone.replace(nonDigitRegex, "")
        val normalized = when {
            digitsOnly.length == WITH_COUNTRY_CODE_LENGTH && digitsOnly.startsWith("91") ->
                digitsOnly.substring(COUNTRY_CODE_PREFIX_LENGTH)
            digitsOnly.length == WITH_LEADING_ZERO_LENGTH && digitsOnly.startsWith("0") ->
                digitsOnly.substring(LEADING_ZERO_PREFIX_LENGTH)
            digitsOnly.length == STANDARD_MOBILE_LENGTH -> digitsOnly
            else -> return null
        }
        return if (indianMobileRegex.matches(normalized)) normalized else null
    }

    fun isValid(rawPhone: String): Boolean = sanitize(rawPhone) != null
}
