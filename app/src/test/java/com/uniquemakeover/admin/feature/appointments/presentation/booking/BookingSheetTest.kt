package com.uniquemakeover.admin.feature.appointments.presentation.booking

import androidx.compose.material3.ExperimentalMaterial3Api
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
class BookingSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun bookingSheet_rendersFormSectionsAndFinancials() {
        val service = Service("s1", "Gold Facial", 800)
        val uiState = BookingUiState(
            isLoading = false,
            availableServices = listOf(service),
            staffList = listOf("Sunita", "Pooja"),
            availableSlots = listOf(Slot("10:00", "10:00 AM", isAvailable = true)),
            draft = BookingDraft(
                name = "Pooja Patil",
                phone = "9822001122",
                selectedServices = listOf(service),
                date = "2026-11-20",
                time = "10:00",
                staff = "Sunita",
                discount = 100,
                advancePaid = 200
            )
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                BookingSheetContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("New Appointment").assertIsDisplayed()
        composeTestRule.onNodeWithText("Client Full Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mobile Number").assertIsDisplayed()
        composeTestRule.onNodeWithText("Services").assertIsDisplayed()
        composeTestRule.onNodeWithText("Appointment Date").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Today").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Tomorrow").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Services Subtotal").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("₹ 800").assertIsDisplayed()
        composeTestRule.onNodeWithText("Final Bill Rate").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹ 700").assertIsDisplayed()
        composeTestRule.onNodeWithText("Due: ₹ 500").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Save Appointment").performScrollTo().assertIsDisplayed()
    }
}
