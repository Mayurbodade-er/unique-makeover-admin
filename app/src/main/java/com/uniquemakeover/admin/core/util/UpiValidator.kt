package com.uniquemakeover.admin.core.util

object UpiValidator {

    // NPCI UPI ID format: username@bankHandle (username: 2-49 chars, handle: 2-49 alpha-numeric)
    private val upiRegex = Regex("^[a-zA-Z0-9.\\-_]{2,49}@[a-zA-Z0-9]{2,49}$")

    fun isValid(rawUpi: String): Boolean {
        val trimmed = rawUpi.trim()
        return upiRegex.matches(trimmed)
    }
}
