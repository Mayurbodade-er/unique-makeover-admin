package com.uniquemakeover.admin.feature.upi.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.uniquemakeover.admin.core.ui.theme.BorderLine
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.TextPrimary
import com.uniquemakeover.admin.feature.upi.domain.model.PaymentPreset

private val CHIP_MIN_HEIGHT = 48.dp
private val CHIP_CORNER_RADIUS = 12.dp
private val DisabledChipBgColor = Color(0xFFF5F5F5)

const val PRESET_CHIP_BALANCE_TEST_TAG = "preset_chip_balance"
const val PRESET_CHIP_ADVANCE_TEST_TAG = "preset_chip_advance"
const val PRESET_CHIP_FULL_TEST_TAG = "preset_chip_full"
const val PRESET_CHIP_CUSTOM_TEST_TAG = "preset_chip_custom"
const val CUSTOM_AMOUNT_INPUT_TEST_TAG = "custom_amount_input"

@Composable
fun PresetSelector(
    selectedPreset: PaymentPreset,
    isAdvanceDisabled: Boolean,
    customAmountInput: String,
    onPresetSelect: (PaymentPreset) -> Unit,
    onCustomAmountChange: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PresetChipsRow(
            selectedPreset = selectedPreset,
            isAdvanceDisabled = isAdvanceDisabled,
            onPresetClick = { preset ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPresetSelect(preset)
            }
        )

        AnimatedVisibility(
            visible = selectedPreset is PaymentPreset.Custom,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CustomAmountInputField(
                amountInput = customAmountInput,
                onAmountChange = onCustomAmountChange
            )
        }
    }
}

@Composable
private fun PresetChipsRow(
    selectedPreset: PaymentPreset,
    isAdvanceDisabled: Boolean,
    onPresetClick: (PaymentPreset) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PresetChip(
            label = "Balance Due",
            isSelected = selectedPreset is PaymentPreset.BalanceDue,
            onClick = { onPresetClick(PaymentPreset.BalanceDue) },
            testTag = PRESET_CHIP_BALANCE_TEST_TAG
        )
        PresetChip(
            label = "Advance (30%)",
            isSelected = selectedPreset is PaymentPreset.Advance30,
            isEnabled = !isAdvanceDisabled,
            onClick = { onPresetClick(PaymentPreset.Advance30) },
            testTag = PRESET_CHIP_ADVANCE_TEST_TAG
        )
        PresetChip(
            label = "Full Payment",
            isSelected = selectedPreset is PaymentPreset.FullPayment,
            onClick = { onPresetClick(PaymentPreset.FullPayment) },
            testTag = PRESET_CHIP_FULL_TEST_TAG
        )
        PresetChip(
            label = "Custom ✏️",
            isSelected = selectedPreset is PaymentPreset.Custom,
            onClick = { onPresetClick(PaymentPreset.Custom()) },
            testTag = PRESET_CHIP_CUSTOM_TEST_TAG
        )
    }
}

@Composable
private fun PresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    isEnabled: Boolean = true
) {
    val containerColor = if (isSelected) BrandPrimary else Color.White
    val contentColor = if (isSelected) Color.White else TextPrimary
    val borderColor = if (isSelected) BrandPrimary else BorderLine

    OutlinedButton(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier
            .heightIn(min = CHIP_MIN_HEIGHT)
            .testTag(testTag),
        shape = RoundedCornerShape(CHIP_CORNER_RADIUS),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = DisabledChipBgColor,
            disabledContentColor = TextMuted
        ),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun CustomAmountInputField(
    amountInput: String,
    onAmountChange: (String) -> Unit
) {
    OutlinedTextField(
        value = amountInput,
        onValueChange = onAmountChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(CUSTOM_AMOUNT_INPUT_TEST_TAG),
        label = { Text("Enter Custom Amount") },
        prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        trailingIcon = {
            if (amountInput.isNotEmpty()) {
                IconButton(onClick = { onAmountChange("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = "Clear amount"
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrandPrimary,
            focusedLabelColor = BrandPrimary
        ),
        shape = RoundedCornerShape(CHIP_CORNER_RADIUS)
    )
}
