package com.uniquemakeover.admin.feature.auth.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uniquemakeover.admin.core.ui.theme.BorderLine
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.SurfaceCard
import com.uniquemakeover.admin.core.ui.theme.TextPrimary

private val BUTTON_SIZE = 72.dp
private const val DISABLED_ALPHA = 0.38f

@Composable
fun PinKeypad(
    onDigitClick: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val keypadRows = listOf(
            listOf('1', '2', '3'),
            listOf('4', '5', '6'),
            listOf('7', '8', '9')
        )

        keypadRows.forEach { rowDigits ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowDigits.forEach { digit ->
                    KeypadDigitButton(
                        digit = digit,
                        enabled = enabled,
                        onClick = {
                            if (enabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDigitClick(digit)
                            }
                        }
                    )
                }
            }
        }

        KeypadBottomRow(
            enabled = enabled,
            onDigitClick = onDigitClick,
            onBackspace = onBackspace
        )
    }
}

@Composable
private fun KeypadBottomRow(
    enabled: Boolean,
    onDigitClick: (Char) -> Unit,
    onBackspace: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(BUTTON_SIZE))

        KeypadDigitButton(
            digit = '0',
            enabled = enabled,
            onClick = {
                if (enabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDigitClick('0')
                }
            }
        )

        KeypadActionButton(
            enabled = enabled,
            onClick = {
                if (enabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBackspace()
                }
            }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Backspace,
                contentDescription = "Backspace",
                tint = BrandPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun KeypadDigitButton(
    digit: Char,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Card(
        modifier = modifier
            .size(BUTTON_SIZE)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, BorderLine),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.size(BUTTON_SIZE),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = digit.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun KeypadActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .size(BUTTON_SIZE)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, BorderLine),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.size(BUTTON_SIZE),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
