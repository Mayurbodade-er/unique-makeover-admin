package com.uniquemakeover.admin.feature.reminders.domain.usecase

import com.uniquemakeover.admin.core.domain.model.Settings
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.reminders.domain.model.TemplateTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FormatReminderTemplateUseCaseTest {

    private lateinit var useCase: FormatReminderTemplateUseCase

    private val sampleSettings = Settings(
        shop = "Unique Makeover",
        wa = "9822001122"
    )

    private val sampleAppointment = Appointment(
        id = "apt-1",
        name = "Priya",
        phone = "9822112233",
        serviceSnapshot = "Facial",
        serviceIds = listOf("srv-1"),
        rate = 800,
        paid = 500,
        staff = "Sunita",
        date = "15-Nov",
        time = "03:00 PM",
        note = "Sensitive skin",
        status = "booked",
        bookedAt = 1000L
    )

    @Before
    fun setUp() {
        useCase = FormatReminderTemplateUseCase()
    }

    @Test
    fun `formats all 8 placeholders cleanly without remaining curly braces`() {
        val template = "Hello {name}, your appointment at {shop} for {service} is on {date} at {time}. " +
            "Total: ₹{rate}, Due: ₹{due}. Call {shopnum}."

        val formatted = useCase(template, sampleAppointment, sampleSettings)

        val expected = "Hello Priya, your appointment at Unique Makeover for Facial is on 15-Nov at 03:00 PM. " +
            "Total: ₹800, Due: ₹300. Call 9822001122."
        assertEquals(expected, formatted)
        assertFalse(formatted.contains("{"))
        assertFalse(formatted.contains("}"))
    }

    @Test
    fun `formats zero balance due cleanly as zero`() {
        val fullyPaidAppointment = sampleAppointment.copy(rate = 800, paid = 800)
        val template = "Due: ₹{due}"

        val formatted = useCase(template, fullyPaidAppointment, sampleSettings)

        assertEquals("Due: ₹0", formatted)
    }

    @Test
    fun `prevents negative balance due when paid exceeds rate`() {
        val overpaidAppointment = sampleAppointment.copy(rate = 800, paid = 1000)
        val template = "Due: ₹{due}"

        val formatted = useCase(template, overpaidAppointment, sampleSettings)

        assertEquals("Due: ₹0", formatted)
    }

    @Test
    fun `preserves emojis and special characters`() {
        val template = "Hi {name}! ✨ Welcome to {shop} 💇‍♀️ & enjoy {service}! Call {shopnum}."

        val formatted = useCase(template, sampleAppointment, sampleSettings)

        val expected = "Hi Priya! ✨ Welcome to Unique Makeover 💇‍♀️ & enjoy Facial! Call 9822001122."
        assertEquals(expected, formatted)
    }

    @Test
    fun `leaves unrecognized tokens intact without crashing`() {
        val template = "Hello {name}, your bill is ₹{rate}. Ref: {booking_code} and {unknown}."

        val formatted = useCase(template, sampleAppointment, sampleSettings)

        assertTrue(formatted.contains("{booking_code}"))
        assertTrue(formatted.contains("{unknown}"))
        assertTrue(formatted.contains("Priya"))
        assertTrue(formatted.contains("800"))
    }
}
