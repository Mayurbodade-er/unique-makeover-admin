package com.uniquemakeover.admin.feature.reminders.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uniquemakeover.admin.core.ui.theme.UniqueMakeoverTheme
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.reminders.domain.model.ReminderQueueItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class RemindersScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleAppointment = Appointment(
        id = "apt-1",
        name = "Priya Sharma",
        phone = "9822112233",
        serviceSnapshot = "Hydra Facial",
        serviceIds = listOf("srv-1"),
        rate = 1200,
        paid = 500,
        staff = "Sunita",
        date = "2026-11-15",
        time = "10:30 AM",
        note = "",
        status = "booked",
        reminded = null,
        bookedAt = 1000L
    )

    @Test
    fun `empty queue displays empty message`() {
        val uiState = RemindersUiState(
            isLoading = false,
            tomorrowDateDisplay = "Tomorrow — Sunday, 15 Nov 2026",
            queue = emptyList(),
            totalCount = 0,
            remindedCount = 0
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                RemindersScreen(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Appointment Reminders").assertIsDisplayed()
        composeTestRule.onNodeWithText("No appointments scheduled for tomorrow").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enjoy your evening! All caught up. ✨").assertIsDisplayed()
    }

    @Test
    fun `queue items render client details and remind button triggers callback`() {
        var clickedItem: ReminderQueueItem? = null
        val item = ReminderQueueItem(sampleAppointment, isReminded = false)

        val uiState = RemindersUiState(
            isLoading = false,
            tomorrowDateDisplay = "Tomorrow — Sunday, 15 Nov 2026",
            queue = listOf(item),
            totalCount = 1,
            remindedCount = 0
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                RemindersScreen(
                    uiState = uiState,
                    actions = RemindersActions(
                        onRemindClick = { clickedItem = it }
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Priya Sharma").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hydra Facial").assertIsDisplayed()
        composeTestRule.onNodeWithText("10:30 AM").assertIsDisplayed()
        composeTestRule.onNodeWithText("Due: ₹700").assertIsDisplayed()
        composeTestRule.onNodeWithText("Not Sent").assertIsDisplayed()

        composeTestRule.onNodeWithText("💬 Remind via WhatsApp").performClick()

        assertTrue(clickedItem != null)
        assertEquals("apt-1", clickedItem?.appointment?.id)
    }
}
