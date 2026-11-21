package com.uniquemakeover.admin.feature.auth.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.ui.theme.UniqueMakeoverTheme
import com.uniquemakeover.admin.feature.auth.domain.model.LockoutState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class PinLockScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun pinLockScreen_rendersHeaderAndKeypadProperly() {
        var clickedChar: Char? = null
        var backspaceClicked = false

        val uiState = PinLockUiState(digitsEntered = 2)

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                PinLockScreen(
                    uiState = uiState,
                    onDigitClick = { clickedChar = it },
                    onBackspace = { backspaceClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Unique Makeover").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enter Admin PIN").assertIsDisplayed()

        // Click digit '5'
        composeTestRule.onNodeWithText("5").assertExists().performClick()
        assertThat(clickedChar).isEqualTo('5')

        // Click backspace
        composeTestRule.onNodeWithContentDescription("Backspace").assertExists().performClick()
        assertThat(backspaceClicked).isTrue()
    }

    @Test
    fun pinLockScreen_whenLocked_showsCountdownAndDisablesKeypad() {
        var clickedChar: Char? = null

        val uiState = PinLockUiState(
            lockoutState = LockoutState.Locked(29),
            isError = true,
            errorMessage = UiText.DynamicString("Too many failed attempts. Try again in 29s")
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                PinLockScreen(
                    uiState = uiState,
                    onDigitClick = { clickedChar = it },
                    onBackspace = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Too many failed attempts. Try again in 29s").assertIsDisplayed()

        // Clicking a digit when disabled should not register a click callback
        composeTestRule.onNodeWithText("5").performClick()
        assertThat(clickedChar).isNull()
    }
}
