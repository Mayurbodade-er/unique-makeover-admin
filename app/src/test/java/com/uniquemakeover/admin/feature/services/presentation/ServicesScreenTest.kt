package com.uniquemakeover.admin.feature.services.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.uniquemakeover.admin.core.domain.model.Service
import com.uniquemakeover.admin.core.ui.theme.UniqueMakeoverTheme
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
}
