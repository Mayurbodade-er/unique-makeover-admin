package com.uniquemakeover.admin.feature.dashboard.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uniquemakeover.admin.core.ui.theme.UniqueMakeoverTheme
import com.uniquemakeover.admin.feature.dashboard.domain.model.AlertBanner
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DashboardScreenAlertsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dashboardScreen_rendersAlertBannersAndHandlesActions() {
        var actionBanner: AlertBanner? = null
        var dismissedBanner: AlertBanner? = null

        val banner = AlertBanner.WaNotConfigured
        val uiState = DashboardUiState(
            todayDate = "2026-11-21",
            alertBanners = listOf(banner)
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                DashboardScreen(
                    uiState = uiState,
                    actions = DashboardActions(
                        alerts = DashboardAlertActions(
                            onActionClick = { actionBanner = it },
                            onDismissClick = { dismissedBanner = it }
                        )
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("WhatsApp Not Configured").assertIsDisplayed()
        composeTestRule.onNodeWithText("WhatsApp number not configured. Tap to setup.").assertIsDisplayed()

        composeTestRule.onNodeWithText("Setup WhatsApp →").performClick()
        assertEquals(banner, actionBanner)

        composeTestRule.onNodeWithContentDescription("Dismiss").performClick()
        assertEquals(banner, dismissedBanner)
    }
}
