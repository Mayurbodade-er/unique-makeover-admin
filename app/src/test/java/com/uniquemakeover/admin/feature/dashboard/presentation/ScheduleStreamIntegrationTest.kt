package com.uniquemakeover.admin.feature.dashboard.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.uniquemakeover.admin.core.ui.theme.UniqueMakeoverTheme
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.dashboard.domain.model.DashboardKpis
import com.uniquemakeover.admin.feature.dashboard.domain.model.ScheduleItem
import com.uniquemakeover.admin.feature.dashboard.presentation.components.ScheduleCardActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class ScheduleStreamIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val apt10 = Appointment(
        id = "apt_10",
        name = "Apt 10AM",
        phone = "9800000010",
        serviceSnapshot = "Facial",
        serviceIds = listOf("s1"),
        rate = 1000,
        paid = 500,
        staff = "Sunita",
        date = "2026-11-20",
        time = "10:00",
        note = "",
        status = "booked",
        bookedAt = 1000L
    )

    private val apt14 = Appointment(
        id = "apt_14",
        name = "Apt 2PM",
        phone = "9800000014",
        serviceSnapshot = "Hair Spa",
        serviceIds = listOf("s2"),
        rate = 2000,
        paid = 2000,
        staff = "Pooja",
        date = "2026-11-20",
        time = "14:00",
        note = "",
        status = "booked",
        bookedAt = 2000L
    )

    @Test
    fun dashboardScreen_rendersScheduleChronologically() {
        val uiState = DashboardUiState(
            isLoading = false,
            todayDate = "2026-11-20",
            kpis = DashboardKpis(bookedCount = 2, scheduledVolume = 3000),
            schedule = listOf(
                ScheduleItem(apt10, isPastTime = true),
                ScheduleItem(apt14, isPastTime = false)
            )
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                DashboardScreen(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Today's Live Schedule (2)").assertIsDisplayed()
        composeTestRule.onNodeWithText("10:00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Apt 10AM").assertIsDisplayed()
        composeTestRule.onNodeWithText("14:00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Apt 2PM").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_quickActions_triggerCallbacks() {
        var markDoneApt: Appointment? = null
        var recordPaymentApt: Appointment? = null
        var cancelApt: Appointment? = null

        val actions = ScheduleCardActions(
            onMarkDone = { markDoneApt = it },
            onRecordPayment = { recordPaymentApt = it },
            onCancel = { cancelApt = it }
        )

        val uiState = DashboardUiState(
            isLoading = false,
            todayDate = "2026-11-20",
            schedule = listOf(ScheduleItem(apt10, isPastTime = false))
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                DashboardScreen(uiState = uiState, actions = DashboardActions(schedule = actions))
            }
        }

        composeTestRule.onNodeWithContentDescription("Mark Done").performClick()
        assertEquals("apt_10", markDoneApt?.id)

        composeTestRule.onNodeWithContentDescription("Record Payment").performClick()
        assertEquals("apt_10", recordPaymentApt?.id)

        composeTestRule.onNodeWithContentDescription("Cancel").performClick()
        assertEquals("apt_10", cancelApt?.id)
    }

    @Test
    fun dashboardScreen_emptySchedule_rendersEmptyBanner() {
        val uiState = DashboardUiState(
            isLoading = false,
            todayDate = "2026-11-20",
            schedule = emptyList()
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                DashboardScreen(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("No appointments scheduled for today").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tap '+' to create a new booking.").assertIsDisplayed()
    }
}
