package com.uniquemakeover.admin.feature.settings.presentation.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.uniquemakeover.admin.core.ui.theme.UniqueMakeoverTheme
import com.uniquemakeover.admin.feature.settings.domain.model.BrandingConfig
import com.uniquemakeover.admin.feature.settings.presentation.BrandingDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrandingSectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun brandingSection_rendersFields_andInvokesSave() {
        val initialConfig = BrandingConfig(
            shopName = "Unique Makeover",
            tagline = "Luxury Beauty Parlour"
        )
        var savedDraft: BrandingDraft? = null

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    BrandingSection(
                        initialConfig = initialConfig,
                        onSaveBranding = { savedDraft = it }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Salon Branding & Identity").assertIsDisplayed()
        composeTestRule.onNodeWithText("Boutique Logo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unique Makeover").assertIsDisplayed()
        composeTestRule.onNodeWithText("Luxury Beauty Parlour").assertIsDisplayed()
        composeTestRule.onNodeWithText("Background Wallpaper").performScrollTo().assertIsDisplayed()

        // Edit shop name
        composeTestRule.onNodeWithText("Unique Makeover").performTextReplacement("Unique Makeover Studio")

        // Click save
        composeTestRule.onNodeWithText("Save Branding Details").performScrollTo().performClick()

        assertNotNull("Save callback should be invoked", savedDraft)
        assertEquals("Unique Makeover Studio", savedDraft?.shopName)
        assertEquals("Luxury Beauty Parlour", savedDraft?.tagline)
    }
}
