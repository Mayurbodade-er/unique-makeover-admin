package com.uniquemakeover.admin.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.uniquemakeover.admin.core.ui.theme.UniqueMakeoverTheme
import com.uniquemakeover.admin.feature.services.presentation.ServicesScreen
import com.uniquemakeover.admin.feature.services.presentation.ServicesUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class UniqueMakeoverNavHostTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun routes_constantsMatchSpecification() {
        assert(Routes.PinLock == "pin_lock")
        assert(Routes.Services == "services")
    }

    @Test
    fun servicesScreen_destinationRendersServicesCatalogTitle() {
        val uiState = ServicesUiState(
            isLoading = false,
            services = emptyList()
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                ServicesScreen(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Services Catalog").assertIsDisplayed()
    }
}
