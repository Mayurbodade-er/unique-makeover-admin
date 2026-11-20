package com.uniquemakeover.admin.feature.upi.presentation.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.uniquemakeover.admin.core.ui.theme.UniqueMakeoverTheme
import com.uniquemakeover.admin.feature.upi.domain.model.PaymentPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PresetSelectorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun presetSelector_rendersAllChips() {
        composeTestRule.setContent {
            UniqueMakeoverTheme {
                PresetSelector(
                    selectedPreset = PaymentPreset.BalanceDue,
                    isAdvanceDisabled = false,
                    customAmountInput = "",
                    onPresetSelect = {},
                    onCustomAmountChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Balance Due").assertIsDisplayed()
        composeTestRule.onNodeWithText("Advance (30%)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Full Payment").assertIsDisplayed()
        composeTestRule.onNodeWithText("Custom ✏️").assertIsDisplayed()
    }

    @Test
    fun presetSelector_advanceTap_triggersCallback() {
        var selectedPreset: PaymentPreset? = null

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                PresetSelector(
                    selectedPreset = PaymentPreset.BalanceDue,
                    isAdvanceDisabled = false,
                    customAmountInput = "",
                    onPresetSelect = { selectedPreset = it },
                    onCustomAmountChange = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(PRESET_CHIP_ADVANCE_TEST_TAG).performClick()
        assertEquals(PaymentPreset.Advance30, selectedPreset)
    }

    @Test
    fun presetSelector_advanceDisabled_whenAlreadyCovered() {
        composeTestRule.setContent {
            UniqueMakeoverTheme {
                PresetSelector(
                    selectedPreset = PaymentPreset.BalanceDue,
                    isAdvanceDisabled = true,
                    customAmountInput = "",
                    onPresetSelect = {},
                    onCustomAmountChange = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(PRESET_CHIP_ADVANCE_TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun presetSelector_customSelected_showsInputFieldAndAcceptsInput() {
        var typedAmount = ""

        composeTestRule.setContent {
            UniqueMakeoverTheme {
                PresetSelector(
                    selectedPreset = PaymentPreset.Custom(0),
                    isAdvanceDisabled = false,
                    customAmountInput = typedAmount,
                    onPresetSelect = {},
                    onCustomAmountChange = { typedAmount = it }
                )
            }
        }

        composeTestRule.onNodeWithTag(CUSTOM_AMOUNT_INPUT_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CUSTOM_AMOUNT_INPUT_TEST_TAG).performTextInput("750")
        assertEquals("750", typedAmount)
    }
}
