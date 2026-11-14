package com.uniquemakeover.admin.feature.services.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.ui.theme.UniqueMakeoverTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ServicesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun servicesScreen_rendersServiceCardsWithTitleAndPrice() {
        val testServices = listOf(
            Service(id = "s1", name = "Haircut", rate = 150),
            Service(id = "s3", name = "Gold Facial", rate = 800)
        )
        val uiState = ServicesUiState(
            isLoading = false,
            services = testServices
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                ServicesScreen(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Services Catalog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Haircut").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹ 150").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gold Facial").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹ 800").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Add Service").assertIsDisplayed()
    }

    @Test
    fun servicesScreen_addFabClick_triggersCallback() {
        var addClicked = false
        val uiState = ServicesUiState(isLoading = false)

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                ServicesScreen(
                    uiState = uiState,
                    onAddClick = { addClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Add Service").performClick()
        assertTrue(addClicked)
    }

    @Test
    fun servicesScreen_serviceCardClick_triggersOnServiceClick() {
        var clickedId = ""
        val testServices = listOf(
            Service(id = "s_facial", name = "Gold Facial", rate = 800)
        )
        val uiState = ServicesUiState(isLoading = false, services = testServices)

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                ServicesScreen(
                    uiState = uiState,
                    onServiceClick = { clickedId = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Gold Facial").performClick()
        assertEquals("s_facial", clickedId)
    }

    @Test
    fun servicesScreen_rendersEmptyState_whenNoServices() {
        val uiState = ServicesUiState(
            isLoading = false,
            services = emptyList()
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                ServicesScreen(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("No services found").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tap '+' to create your first service").assertIsDisplayed()
    }

    @Test
    fun serviceEditorSheet_rendersFormElements() {
        val uiState = ServicesUiState(
            isEditorOpen = true,
            editorName = "O3+ Diamond Facial",
            editorRate = "1200"
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                ServiceEditorSheet(
                    uiState = uiState,
                    actions = ServiceEditorActions(
                        onNameChange = {},
                        onRateChange = {},
                        onSave = {},
                        onDelete = {},
                        onDismiss = {}
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Add Service").assertIsDisplayed()
        composeTestRule.onNodeWithText("Service Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("O3+ Diamond Facial").assertIsDisplayed()
        composeTestRule.onNodeWithText("Service Rate").assertIsDisplayed()
        composeTestRule.onNodeWithText("1200").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save Service").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }
}
