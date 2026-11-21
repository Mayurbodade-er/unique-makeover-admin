package com.uniquemakeover.admin.feature.settings.presentation.sections

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
class DataManagementSectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dataManagementSection_rendersHeaderAndCardCorrectly() {
        composeTestRule.setContent {
            UniqueMakeoverTheme {
                DataManagementCardContent(
                    lastBackup = "2026-11-14",
                    isExporting = false,
                    onExportClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Full Database Export").assertIsDisplayed()
        composeTestRule.onNodeWithText("Last Backup: 2026-11-14").assertIsDisplayed()
        composeTestRule.onNodeWithText(" Export Backup (JSON)").assertIsDisplayed()
    }

    @Test
    fun dataManagementSection_whenLastBackupNull_displaysNever() {
        composeTestRule.setContent {
            UniqueMakeoverTheme {
                DataManagementCardContent(
                    lastBackup = null,
                    isExporting = false,
                    onExportClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Last Backup: Never").assertIsDisplayed()
    }

    @Test
    fun dataManagementSection_whenExporting_displaysExportingIndicator() {
        composeTestRule.setContent {
            UniqueMakeoverTheme {
                DataManagementCardContent(
                    lastBackup = "2026-11-14",
                    isExporting = true,
                    onExportClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Exporting...").assertIsDisplayed()
    }
}
