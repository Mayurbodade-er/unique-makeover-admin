package com.uniquemakeover.admin.core.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uniquemakeover.admin.core.ui.theme.UniqueMakeoverTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConfirmDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun confirmDialog_rendersTitleMessageAndButtons() {
        composeTestRule.setContent {
            UniqueMakeoverTheme {
                ConfirmDialog(
                    config = ConfirmDialogConfig(
                        title = "Delete Appointment?",
                        message = "This will permanently delete the appointment for Sunita Sharma on 20-Nov."
                    ),
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Delete Appointment?").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "This will permanently delete the appointment for Sunita Sharma on 20-Nov."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").assertIsDisplayed()
    }

    @Test
    fun confirmDialog_cancelClick_triggersDismissWithoutConfirm() {
        var confirmed = false
        var dismissed = false

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                ConfirmDialog(
                    config = ConfirmDialogConfig(
                        title = "Delete Appointment?",
                        message = "Permanent delete test"
                    ),
                    onConfirm = { confirmed = true },
                    onDismiss = { dismissed = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Cancel").performClick()

        assertTrue(dismissed)
        assertFalse(confirmed)
    }

    @Test
    fun confirmDialog_confirmClick_triggersConfirm() {
        var confirmed = false
        var dismissed = false

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                ConfirmDialog(
                    config = ConfirmDialogConfig(
                        title = "Delete Appointment?",
                        message = "Permanent delete test"
                    ),
                    onConfirm = { confirmed = true },
                    onDismiss = { dismissed = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Delete").performClick()

        assertTrue(confirmed)
        assertFalse(dismissed)
    }
}
