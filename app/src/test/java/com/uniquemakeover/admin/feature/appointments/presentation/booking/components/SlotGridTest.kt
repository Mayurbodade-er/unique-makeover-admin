package com.uniquemakeover.admin.feature.appointments.presentation.booking.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uniquemakeover.admin.core.domain.model.Slot
import com.uniquemakeover.admin.core.domain.model.UnavailableReason
import com.uniquemakeover.admin.core.ui.theme.UniqueMakeoverTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class SlotGridTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun slotGrid_availableSlot_invokesCallbackOnTap() {
        var selectedSlot: Slot? = null
        val slots = listOf(
            Slot("10:00", "10:00 AM", isAvailable = true),
            Slot("10:30", "10:30 AM", isAvailable = true)
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                SlotGrid(
                    slots = slots,
                    selectedSlot = null,
                    onSlotSelected = { selectedSlot = it }
                )
            }
        }

        composeTestRule.onNodeWithText("10:00 AM").assertIsDisplayed()
        composeTestRule.onNodeWithText("10:00 AM").performClick()

        assertEquals("10:00", selectedSlot?.time24)
    }

    @Test
    fun slotGrid_bookedSlot_displaysFullBadgeAndDisablesClicks() {
        var clicked = false
        val slots = listOf(
            Slot(
                time24 = "11:30",
                time12 = "11:30 AM",
                isAvailable = false,
                reason = UnavailableReason.ALREADY_BOOKED
            )
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                SlotGrid(
                    slots = slots,
                    selectedSlot = null,
                    onSlotSelected = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Full").assertIsDisplayed()
        composeTestRule.onNodeWithText("11:30 AM").performClick()

        assertFalse(clicked)
    }

    @Test
    fun slotGrid_pastSlot_displaysPastBadgeAndDisablesClicks() {
        var clicked = false
        val slots = listOf(
            Slot(
                time24 = "09:00",
                time12 = "09:00 AM",
                isAvailable = false,
                reason = UnavailableReason.PAST_TIME
            )
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                SlotGrid(
                    slots = slots,
                    selectedSlot = null,
                    onSlotSelected = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Past").assertIsDisplayed()
        composeTestRule.onNodeWithText("09:00 AM").performClick()

        assertFalse(clicked)
    }

    @Test
    @Config(qualifiers = "w900dp-h1200dp")
    fun slotGrid_tabletLayout_rendersProperly() {
        val slots = (10..15).map { hour ->
            Slot("%02d:00".format(hour), "%02d:00 PM".format(hour), isAvailable = true)
        }

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                SlotGrid(
                    slots = slots,
                    selectedSlot = slots.first(),
                    onSlotSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithText("10:00 PM").assertIsDisplayed()
    }
}
