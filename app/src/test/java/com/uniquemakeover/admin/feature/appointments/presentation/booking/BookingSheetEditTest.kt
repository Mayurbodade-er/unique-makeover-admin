package com.uniquemakeover.admin.feature.appointments.presentation.booking

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.domain.model.Slot
import com.uniquemakeover.admin.core.ui.theme.UniqueMakeoverTheme
import com.uniquemakeover.admin.feature.appointments.domain.model.BookingDraft
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class BookingSheetEditTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bookingSheetEditMode_prePopulatesValuesAndRendersEditTitles() {
        val service = Service("s1", "Gold Facial", 800)
        val uiState = BookingUiState(
            isLoading = false,
            editingAppointmentId = "apt_101",
            isDateTimeLocked = false,
            availableServices = listOf(service),
            staffList = listOf("Sunita"),
            availableSlots = listOf(Slot("11:00", "11:00 AM", isAvailable = true)),
            draft = BookingDraft(
                name = "Pooja Patil",
                phone = "9822001122",
                selectedServices = listOf(service),
                date = "2026-11-25",
                time = "11:00",
                staff = "Sunita",
                discount = 100,
                advancePaid = 200,
                internalNote = "Prefers lukewarm water"
            )
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                BookingSheetContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Edit Appointment").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pooja Patil").assertIsDisplayed()
        composeTestRule.onNodeWithText("9822001122").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gold Facial (₹800)").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Appointment Date").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Update Appointment").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun bookingSheetEditMode_showsRescheduleLockNoticeWhenStatusLocked() {
        val service = Service("s1", "Gold Facial", 800)
        val uiState = BookingUiState(
            isLoading = false,
            editingAppointmentId = "apt_101",
            isDateTimeLocked = true,
            availableServices = listOf(service),
            staffList = listOf("Sunita"),
            availableSlots = listOf(Slot("11:00", "11:00 AM", isAvailable = true)),
            draft = BookingDraft(
                name = "Pooja Patil",
                phone = "9822001122",
                selectedServices = listOf(service),
                date = "2026-11-25",
                time = "11:00"
            )
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                BookingSheetContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("To reschedule, first restore appointment status to Booked")
            .assertIsDisplayed()
    }
}
