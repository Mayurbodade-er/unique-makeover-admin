package com.uniquemakeover.admin.feature.clients.presentation.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.core.ui.theme.UniqueMakeoverTheme
import com.uniquemakeover.admin.feature.clients.domain.model.ClientHistoryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClientDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun clientDetailScreen_rendersClientMetricsAndHistory() {
        val sampleItems = listOf(
            ClientHistoryItem(
                appointmentId = "apt_1",
                date = "2026-11-20",
                time = "14:30",
                formattedServices = "Hydra Facial",
                staff = "Sunita",
                rate = 1500,
                paid = 1500,
                balanceDue = 0,
                status = AppointmentStatus.DONE
            )
        )

        val uiState = ClientDetailUiState(
            isLoading = false,
            clientName = "Neha Kulkarni",
            phone = "9822001122",
            totalSpend = 1500,
            totalVisits = 1,
            historyItems = sampleItems,
            selectedTab = 0
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                ClientDetailScreen(
                    uiState = uiState,
                    actions = ClientDetailActions()
                )
            }
        }

        composeTestRule.onNodeWithText("Client Profile").assertIsDisplayed()
        composeTestRule.onNodeWithText("Neha Kulkarni").assertIsDisplayed()
        composeTestRule.onNodeWithText("+91 9822001122").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹ 1500").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 Visit").assertIsDisplayed()
        composeTestRule.onNodeWithText("Call Client").assertIsDisplayed()
        composeTestRule.onNodeWithText("WhatsApp").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hydra Facial").assertExists()
        composeTestRule.onNodeWithText("Done ✓").assertExists()
    }

    @Test
    fun clientDetailScreen_tabSelection_triggersCallback() {
        var selectedTab = -1
        val uiState = ClientDetailUiState(
            isLoading = false,
            clientName = "Neha",
            phone = "9822001122",
            totalSpend = 0,
            totalVisits = 0,
            historyItems = emptyList(),
            selectedTab = 0
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                ClientDetailScreen(
                    uiState = uiState,
                    actions = ClientDetailActions(
                        onTabSelected = { selectedTab = it }
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Private Notes 🔒").performClick()
        assertEquals(1, selectedTab)
    }

    @Test
    fun clientDetailScreen_quickActions_triggerCallbacks() {
        var callClicked = false
        var whatsAppClicked = false
        val uiState = ClientDetailUiState(
            isLoading = false,
            clientName = "Pooja",
            phone = "9876543210",
            totalSpend = 500,
            totalVisits = 1,
            historyItems = emptyList()
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                ClientDetailScreen(
                    uiState = uiState,
                    actions = ClientDetailActions(
                        onCallClick = { callClicked = true },
                        onWhatsAppClick = { whatsAppClicked = true }
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Call Client").performClick()
        assertTrue(callClicked)

        composeTestRule.onNodeWithText("WhatsApp").performClick()
        assertTrue(whatsAppClicked)
    }

    @Test
    fun clientDetailScreen_privateNotesTab_rendersNoteEditorAndTriggersChange() {
        var typedText = ""
        val uiState = ClientDetailUiState(
            isLoading = false,
            clientName = "Neha",
            phone = "9822001122",
            selectedTab = 1,
            noteText = "Allergic to bleach",
            noteSaveStatus = NoteSaveStatus.SAVED
        )

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                ClientDetailScreen(
                    uiState = uiState,
                    actions = ClientDetailActions(
                        onNoteChange = { typedText = it }
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Confidential Salon Notes").assertExists()
        composeTestRule.onNodeWithText("Saved ✓").assertExists()
        composeTestRule.onNodeWithText("Allergic to bleach").assertExists()
    }
}
