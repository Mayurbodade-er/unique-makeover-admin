package com.uniquemakeover.admin.core.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uniquemakeover.admin.core.ui.theme.UniqueMakeoverTheme
import com.uniquemakeover.admin.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class AppScaffoldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appScaffold_phoneLayout_rendersAllNavItems() {
        composeTestRule.setContent {
            UniqueMakeoverTheme {
                AppScaffold(
                    currentRoute = Routes.Services,
                    onNavigate = {}
                ) {
                    Text("Services Content")
                }
            }
        }

        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithText("Appointments").assertIsDisplayed()
        composeTestRule.onNodeWithText("Services").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reminders").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Services Content").assertIsDisplayed()
    }

    @Test
    fun appScaffold_clickNavItem_invokesOnNavigate() {
        var navigatedTo = ""

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                AppScaffold(
                    currentRoute = Routes.Services,
                    onNavigate = { navigatedTo = it }
                ) {
                    Text("Services Content")
                }
            }
        }

        composeTestRule.onNodeWithText("Settings").performClick()
        assertEquals(Routes.Settings, navigatedTo)
    }

    @Test
    @Config(qualifiers = "w900dp-h1200dp")
    fun appScaffold_tabletLayout_rendersNavigationRail() {
        composeTestRule.setContent {
            UniqueMakeoverTheme {
                AppScaffold(
                    currentRoute = Routes.Services,
                    onNavigate = {}
                ) {
                    Text("Tablet Content")
                }
            }
        }

        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tablet Content").assertIsDisplayed()
    }
}
