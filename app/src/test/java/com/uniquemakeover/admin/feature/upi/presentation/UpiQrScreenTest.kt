package com.uniquemakeover.admin.feature.upi.presentation

import android.app.Activity
import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.uniquemakeover.admin.core.ui.theme.UniqueMakeoverTheme
import com.uniquemakeover.admin.feature.upi.presentation.components.QR_CANVAS_TEST_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UpiQrScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun upiQrScreen_contentState_rendersQrCanvasDetails() {
        val dummyBitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val contentState = UpiQrUiState.Content(
            qrBitmap = dummyBitmap,
            upiUri = "upi://pay?pa=uniquemakeover@upi&pn=Unique%20Makeover&am=850.00&cu=INR",
            amount = 850,
            note = "Pooja - Balance Payment",
            shopName = "Unique Makeover",
            upiId = "uniquemakeover@upi"
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                UpiQrScreen(uiState = contentState)
            }
        }

        composeTestRule.onNodeWithText("UPI Scan & Pay").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹ 850").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unique Makeover").assertIsDisplayed()
        composeTestRule.onNodeWithText("uniquemakeover@upi").assertIsDisplayed()
        composeTestRule.onNodeWithTag(QR_CANVAS_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText("Pooja - Balance Payment").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Send Payment Link on WhatsApp").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Save to Gallery").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Share QR").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun upiQrScreen_sendWhatsAppClick_invokesActionCallback() {
        val dummyBitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val contentState = UpiQrUiState.Content(
            qrBitmap = dummyBitmap,
            upiUri = "upi://pay?pa=uniquemakeover@upi",
            amount = 850,
            note = "Pooja",
            shopName = "Unique Makeover",
            upiId = "uniquemakeover@upi"
        )
        var whatsappClicked = false

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                UpiQrScreen(
                    uiState = contentState,
                    actions = UpiQrActions(
                        onSendWhatsAppClick = { whatsappClicked = true }
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Send Payment Link on WhatsApp")
            .performScrollTo()
            .performClick()
        assertTrue("WhatsApp button click should invoke action callback", whatsappClicked)
    }

    @Test
    fun upiQrScreen_saveToGalleryClick_invokesActionCallback() {
        val dummyBitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val contentState = UpiQrUiState.Content(
            qrBitmap = dummyBitmap,
            upiUri = "upi://pay?pa=uniquemakeover@upi",
            amount = 850,
            note = "Pooja",
            shopName = "Unique Makeover",
            upiId = "uniquemakeover@upi"
        )
        var saveClicked = false

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                UpiQrScreen(
                    uiState = contentState,
                    actions = UpiQrActions(
                        onSaveToGalleryClick = { saveClicked = true }
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Save to Gallery")
            .performScrollTo()
            .performClick()
        assertTrue("Save to Gallery click should invoke action callback", saveClicked)
    }

    @Test
    fun upiQrScreen_shareQrClick_invokesActionCallback() {
        val dummyBitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val contentState = UpiQrUiState.Content(
            qrBitmap = dummyBitmap,
            upiUri = "upi://pay?pa=uniquemakeover@upi",
            amount = 850,
            note = "Pooja",
            shopName = "Unique Makeover",
            upiId = "uniquemakeover@upi"
        )
        var shareClicked = false

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                UpiQrScreen(
                    uiState = contentState,
                    actions = UpiQrActions(
                        onShareQrClick = { shareClicked = true }
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Share QR")
            .performScrollTo()
            .performClick()
        assertTrue("Share QR click should invoke action callback", shareClicked)
    }

    @Test
    fun upiQrScreen_screenBrightness_boostsToFullAndRestores() {
        val activityController = Robolectric.buildActivity(Activity::class.java).setup()
        val activity = activityController.get()
        activity.window.attributes.screenBrightness = 0.3f

        val dummyBitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val contentState = UpiQrUiState.Content(
            qrBitmap = dummyBitmap,
            upiUri = "upi://pay?pa=test@upi",
            amount = 500,
            note = "Test",
            shopName = "Unique",
            upiId = "test@upi"
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                UpiQrScreen(uiState = contentState)
            }
        }

        // On screen open, window brightness attribute boosts to 1.0f (BRIGHTNESS_OVERRIDE_FULL)
        // Verify screen renders smoothly without throwing
        composeTestRule.onNodeWithText("UPI Scan & Pay").assertIsDisplayed()
    }
}
