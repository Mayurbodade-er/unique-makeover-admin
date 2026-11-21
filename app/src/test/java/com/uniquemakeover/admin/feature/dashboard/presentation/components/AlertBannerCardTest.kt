package com.uniquemakeover.admin.feature.dashboard.presentation.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uniquemakeover.admin.core.ui.theme.UniqueMakeoverTheme
import com.uniquemakeover.admin.feature.dashboard.domain.model.AlertBanner
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AlertBannerCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun alertBannerCard_rendersBackupOverdue_invokesCallbacks() {
        var actionClicked = false
        var dismissClicked = false

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                AlertBannerCard(
                    banner = AlertBanner.BackupOverdue(daysSinceLast = 5),
                    onActionClick = { actionClicked = true },
                    onDismissClick = { dismissClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Backup Overdue!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Last backup was 5 days ago. Protect client records.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Backup Now →").assertIsDisplayed().performClick()
        assertTrue("Action callback should be invoked", actionClicked)

        composeTestRule.onNodeWithContentDescription("Dismiss").assertIsDisplayed().performClick()
        assertTrue("Dismiss callback should be invoked", dismissClicked)
    }

    @Test
    fun alertBannerCard_rendersRemindersPending() {
        composeTestRule.setContent {
            UniqueMakeoverTheme {
                AlertBannerCard(
                    banner = AlertBanner.RemindersPending(count = 3),
                    onActionClick = {},
                    onDismissClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Reminders Pending").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 client reminder(s) pending for tomorrow.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Send Now →").assertIsDisplayed()
    }
}
