package com.uniquemakeover.admin.feature.services.presentation.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uniquemakeover.admin.core.ui.theme.UniqueMakeoverTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PhotoPickerFieldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun photoPickerField_noPhoto_rendersAddPhotoBadge_andShowsOptionsOnClick() {
        composeTestRule.setContent {
            UniqueMakeoverTheme {
                PhotoPickerField(
                    imagePath = null,
                    onPhotoSelected = {},
                    onPhotoRemoved = {},
                    onPermissionDenied = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Add Photo").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Edit Photo Badge").assertIsDisplayed()

        // Tap to open chooser
        composeTestRule.onNodeWithContentDescription("Add Photo").performClick()

        composeTestRule.onNodeWithText("Service Photo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Take Photo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Choose from Gallery").assertIsDisplayed()
    }

    @Test
    fun photoPickerField_withCustomPhoto_showsRemoveOptionInChooser() {
        composeTestRule.setContent {
            UniqueMakeoverTheme {
                PhotoPickerField(
                    imagePath = "images/custom_spa.jpg",
                    onPhotoSelected = {},
                    onPhotoRemoved = {},
                    onPermissionDenied = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Edit Photo Badge").performClick()

        composeTestRule.onNodeWithText("Take Photo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Choose from Gallery").assertIsDisplayed()
        composeTestRule.onNodeWithText("Remove Photo").assertIsDisplayed()
    }
}
