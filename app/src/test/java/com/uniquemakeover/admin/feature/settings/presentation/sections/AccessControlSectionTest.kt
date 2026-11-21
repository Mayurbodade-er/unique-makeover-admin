package com.uniquemakeover.admin.feature.settings.presentation.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.uniquemakeover.admin.core.ui.theme.UniqueMakeoverTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class AccessControlSectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun accessControlSection_rendersCardContentProperly() {
        composeTestRule.setContent {
            UniqueMakeoverTheme {
                AccessControlSection(
                    onChangePin = { _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("🔒 Security & Access Control").assertIsDisplayed()
        composeTestRule.onNodeWithText("Admin Access PIN").assertIsDisplayed()
        composeTestRule.onNodeWithText("Protected with PBKDF2 & Android Keystore").assertIsDisplayed()
        composeTestRule.onNodeWithText(" Change PIN").assertIsDisplayed()
    }

    @Test
    fun changePinFormFields_rendersInputsAndLabels() {
        composeTestRule.setContent {
            UniqueMakeoverTheme {
                ChangePinFormFields(
                    state = PinDialogState(),
                    onStateChange = {},
                    serverErrorMessage = null
                )
            }
        }

        composeTestRule.onNodeWithText("Current 4-digit PIN").assertIsDisplayed()
        composeTestRule.onNodeWithText("New 4-digit PIN").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm New PIN").assertIsDisplayed()
    }

    @Test
    fun pinFormInlineError_displaysAllValidationStates() {
        composeTestRule.setContent {
            UniqueMakeoverTheme {
                Column {
                    PinFormInlineError(
                        isSameAsCurrent = true,
                        isMismatch = false,
                        serverErrorMessage = null
                    )
                    PinFormInlineError(
                        isSameAsCurrent = false,
                        isMismatch = true,
                        serverErrorMessage = null
                    )
                    PinFormInlineError(
                        isSameAsCurrent = false,
                        isMismatch = false,
                        serverErrorMessage = "Incorrect current PIN"
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("New PIN must be different from current PIN").assertIsDisplayed()
        composeTestRule.onNodeWithText("PINs do not match").assertIsDisplayed()
        composeTestRule.onNodeWithText("Incorrect current PIN").assertIsDisplayed()
    }
}
