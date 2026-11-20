package com.uniquemakeover.admin.feature.upi.presentation.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uniquemakeover.admin.core.ui.theme.UniqueMakeoverTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UpiBlockingBannerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun upiBlockingBanner_rendersHeadlineAndExplanation() {
        composeTestRule.setContent {
            UniqueMakeoverTheme {
                UpiBlockingBanner(
                    onConfigureUpiClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Merchant UPI ID Required").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "To generate offline scan-and-pay QR codes for Google Pay, PhonePe, and Paytm, " +
                "please configure your merchant UPI address in Settings."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Configure UPI in Settings").assertIsDisplayed()
    }

    @Test
    fun upiBlockingBanner_buttonClick_triggersCallback() {
        var clicked = false

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                UpiBlockingBanner(
                    onConfigureUpiClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Configure UPI in Settings").performClick()
        assertTrue("Expected onConfigureUpiClick to be called on button click", clicked)
    }
}
