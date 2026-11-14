package com.uniquemakeover.admin.feature.appointments.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uniquemakeover.admin.core.ui.theme.UniqueMakeoverTheme
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.appointments.domain.model.AppointmentFilter
import com.uniquemakeover.admin.feature.appointments.domain.model.FilterTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private val sampleAppointment = Appointment(
        id = "apt_1",
        name = "Sunita Sharma",
        phone = "9876543210",
        serviceSnapshot = "Gold Facial + Hair Spa",
        serviceIds = listOf("s1", "s2"),
        rate = 1200,
        paid = 400,
        staff = "Sunita",
        date = "2026-11-20",
        time = "10:00",
        note = "",
        status = "booked",
        reminded = null,
        bookedAt = 1000L
    )

    @Test
    fun appointmentsScreen_rendersTopBarFilterTabsAndAppointmentCard() {
        var clickedAppointment: Appointment? = null
        var selectedTab: FilterTab? = null

        val uiState = AppointmentsUiState(
            isLoading = false,
            appointments = listOf(sampleAppointment),
            filter = AppointmentFilter(tab = FilterTab.UPCOMING),
            tabCounts = TabCounts(all = 1, upcoming = 1, completed = 0, cancelled = 0)
        )

        val actions = AppointmentsActions(
            onTabSelected = { selectedTab = it },
            onAppointmentClick = { clickedAppointment = it }
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                AppointmentsScreen(
                    uiState = uiState,
                    actions = actions
                )
            }
        }

        composeTestRule.onNodeWithText("Appointments").assertIsDisplayed()
        composeTestRule.onNodeWithText("Search client name or mobile...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Upcoming").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sunita Sharma").assertIsDisplayed()
        composeTestRule.onNodeWithText("+91 9876543210").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gold Facial + Hair Spa").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rate: ₹1200").assertIsDisplayed()
        composeTestRule.onNodeWithText("Due: ₹800").assertIsDisplayed()
        composeTestRule.onNodeWithText("Book Appointment", useUnmergedTree = true).assertExists()

        composeTestRule.onNodeWithText("Completed").performClick()
        assertEquals(FilterTab.COMPLETED, selectedTab)

        composeTestRule.onNodeWithText("Sunita Sharma").performClick()
        assertEquals("apt_1", clickedAppointment?.id)
    }

    @Test
    fun appointmentsScreen_emptyList_rendersEmptyState() {
        var clearFiltersClicked = false
        val uiState = AppointmentsUiState(
            isLoading = false,
            appointments = emptyList(),
            filter = AppointmentFilter(tab = FilterTab.CANCELLED)
        )

        val actions = AppointmentsActions(
            onClearFilters = { clearFiltersClicked = true }
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                AppointmentsScreen(
                    uiState = uiState,
                    actions = actions
                )
            }
        }

        composeTestRule.onNodeWithText("No appointments found").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clear Filters").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clear Filters").performClick()
        assertTrue(clearFiltersClicked)
    }
}
