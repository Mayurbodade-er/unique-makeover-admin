package com.uniquemakeover.admin.feature.auth.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.DangerCancel
import com.uniquemakeover.admin.core.ui.theme.InputBorder
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import kotlin.math.roundToInt

private const val TOTAL_PIN_DOTS = 4
private const val SHAKE_ANIMATION_DURATION_MS = 60
private const val SHAKE_OFFSET_PX = 20f
private const val DOT_COLOR_ANIMATION_MS = 150

@Composable
fun PinDots(
    digitsEntered: Int,
    isError: Boolean,
    shakeTrigger: Long,
    modifier: Modifier = Modifier
) {
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger > 0L) {
            shakeOffset.animateTo(SHAKE_OFFSET_PX, tween(SHAKE_ANIMATION_DURATION_MS))
            shakeOffset.animateTo(-SHAKE_OFFSET_PX, tween(SHAKE_ANIMATION_DURATION_MS))
            shakeOffset.animateTo(SHAKE_OFFSET_PX / 2, tween(SHAKE_ANIMATION_DURATION_MS))
            shakeOffset.animateTo(-SHAKE_OFFSET_PX / 2, tween(SHAKE_ANIMATION_DURATION_MS))
            shakeOffset.animateTo(0f, tween(SHAKE_ANIMATION_DURATION_MS))
        }
    }

    Row(
        modifier = modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until TOTAL_PIN_DOTS) {
            val isFilled = i < digitsEntered
            val targetColor = when {
                isError -> DangerCancel
                isFilled -> LuxuryGold
                else -> Color.Transparent
            }
            val dotColor by animateColorAsState(
                targetValue = targetColor,
                animationSpec = tween(DOT_COLOR_ANIMATION_MS),
                label = "PinDotColor"
            )

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(
                        width = 1.5.dp,
                        color = if (isError) DangerCancel else if (isFilled) BrandPrimary else InputBorder,
                        shape = CircleShape
                    )
            )
        }
    }
}
