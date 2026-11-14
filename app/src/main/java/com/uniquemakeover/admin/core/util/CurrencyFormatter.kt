package com.uniquemakeover.admin.core.util

private const val THOUSAND_DIVISOR = 1000
private const val HUNDRED_DIVISOR = 100
private const val THREE_DIGIT_PAD = 3
private const val TWO_DIGIT_PAD = 2

object CurrencyFormatter {

    fun formatInr(amount: Int): String {
        if (amount == 0) return "₹ 0"

        val isNegative = amount < 0
        val absAmount = if (isNegative) -amount else amount

        val formatted = if (absAmount < THOUSAND_DIVISOR) {
            absAmount.toString()
        } else {
            val lastThree = (absAmount % THOUSAND_DIVISOR).toString().padStart(THREE_DIGIT_PAD, '0')
            var remaining = absAmount / THOUSAND_DIVISOR
            val groups = mutableListOf<String>()

            while (remaining > 0) {
                if (remaining >= HUNDRED_DIVISOR) {
                    val twoDigits = (remaining % HUNDRED_DIVISOR).toString().padStart(TWO_DIGIT_PAD, '0')
                    groups.add(0, twoDigits)
                    remaining /= HUNDRED_DIVISOR
                } else {
                    groups.add(0, remaining.toString())
                    remaining = 0
                }
            }

            "${groups.joinToString(",")},$lastThree"
        }

        return if (isNegative) "-₹ $formatted" else "₹ $formatted"
    }
}
