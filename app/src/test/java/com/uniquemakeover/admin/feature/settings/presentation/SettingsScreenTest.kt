package com.uniquemakeover.admin.feature.settings.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.uniquemakeover.admin.core.domain.model.OperationsConfig
import com.uniquemakeover.admin.core.ui.theme.UniqueMakeoverTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsScreen_rendersTitleAndSections() {
        val uiState = SettingsUiState(
            isLoading = false,
            operationsConfig = OperationsConfig(
                openTime = "09:00",
                closeTime = "20:00",
                slotDurationMinutes = 30,
                staffList = listOf("Sunita", "Pooja")
            )
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                SettingsScreen(
                    uiState = uiState,
                    actions = SettingsActions(
                        onSaveOperations = { _, _, _, _ -> }
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Salon Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open Time").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Close Time").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Slot Duration").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Staff Roster").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Save Operations Settings").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun settingsScreen_saveButton_invokesCallback() {
        var clicked = false
        var savedOpen = ""
        var savedClose = ""

        val uiState = SettingsUiState(
            isLoading = false,
            operationsConfig = OperationsConfig(
                openTime = "09:00",
                closeTime = "20:00",
                slotDurationMinutes = 30,
                staffList = listOf("Sunita")
            )
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                SettingsScreen(
                    uiState = uiState,
                    actions = SettingsActions(
                        onSaveOperations = { open, close, _, _ ->
                            clicked = true
                            savedOpen = open
                            savedClose = close
                        }
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Save Operations Settings").performScrollTo().performClick()

        assertTrue(clicked)
        assertEquals("09:00", savedOpen)
        assertEquals("20:00", savedClose)
    }
}
