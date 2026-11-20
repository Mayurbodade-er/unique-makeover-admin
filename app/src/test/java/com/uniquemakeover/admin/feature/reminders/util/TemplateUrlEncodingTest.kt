package com.uniquemakeover.admin.feature.reminders.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class TemplateUrlEncodingTest {

    @Test
    fun `preserves special characters emojis and ampersands after url encoding and decoding`() {
        val rawMessage = "Hello Priya! Your appointment at Unique Makeover for Hair & Facial is on 15-Nov " +
            "at 03:00 PM. Total: ₹800, Due: ₹300. Call 9822001122. See you soon! ✨💇‍♀️ #UniqueBeauty"

        val encoded = URLEncoder.encode(rawMessage, StandardCharsets.UTF_8.toString())
            .replace("+", "%20")

        // Ensure no literal ampersands remain unencoded to prevent WhatsApp URL truncations
        assertTrue(encoded.contains("%26") || !encoded.contains("&"))
        assertTrue(encoded.contains("%20"))

        val decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())
        assertEquals(rawMessage, decoded)
    }

    @Test
    fun `encodes regional Marathi script accurately`() {
        val marathiMessage = "नमस्कार Priya, आपली भेट 15-Nov रोजी 03:00 PM वाजता आहे. Unique Makeover."

        val encoded = URLEncoder.encode(marathiMessage, StandardCharsets.UTF_8.toString())
        val decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())

        assertEquals(marathiMessage, decoded)
    }
}
