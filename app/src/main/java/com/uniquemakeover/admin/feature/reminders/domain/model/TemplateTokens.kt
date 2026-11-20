package com.uniquemakeover.admin.feature.reminders.domain.model

object TemplateTokens {
    const val NAME = "{name}"
    const val SHOP = "{shop}"
    const val SERVICE = "{service}"
    const val DATE = "{date}"
    const val TIME = "{time}"
    const val RATE = "{rate}"
    const val DUE = "{due}"
    const val SHOP_NUM = "{shopnum}"

    val ALL_TOKENS = listOf(
        NAME,
        SHOP,
        SERVICE,
        DATE,
        TIME,
        RATE,
        DUE,
        SHOP_NUM
    )

    const val DEFAULT_TEMPLATE =
        "Hello {name}, your appointment at {shop} for {service} is scheduled for tomorrow " +
            "({date}) at {time}. Total bill: ₹{rate}, Balance due: ₹{due}. " +
            "For changes, call {shopnum}. See you soon! ✨"
}
