package com.uniquemakeover.admin.feature.appointments.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uniquemakeover.admin.core.domain.model.Slot
import com.uniquemakeover.admin.core.ui.theme.UniqueMakeoverTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class AppointmentsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appointmentsScreen_rendersTopBarAndSlotGrid() {
        var selectedSlot: Slot? = null
        val uiState = AppointmentsUiState(
            isLoading = false,
            selectedDate = "2026-11-20",
            slots = listOf(
                Slot("10:00", "10:00 AM", isAvailable = true),
                Slot("10:30", "10:30 AM", isAvailable = true)
            )
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                AppointmentsScreen(
                    uiState = uiState,
                    onSelectSlot = { selectedSlot = it },
                    onSelectDate = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Appointments").assertIsDisplayed()
        composeTestRule.onNodeWithText("Available Appointment Slots").assertIsDisplayed()
        composeTestRule.onNodeWithText("Book Appointment", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("10:00 AM").assertIsDisplayed()
        composeTestRule.onNodeWithText("10:00 AM").performClick()

        assertEquals("10:00", selectedSlot?.time24)
    }
}
