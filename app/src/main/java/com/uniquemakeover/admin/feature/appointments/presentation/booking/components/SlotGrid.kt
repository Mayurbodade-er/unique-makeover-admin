package com.uniquemakeover.admin.feature.appointments.presentation.booking.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uniquemakeover.admin.core.domain.model.Slot
import com.uniquemakeover.admin.core.domain.model.UnavailableReason
import com.uniquemakeover.admin.core.ui.theme.BorderLine
import com.uniquemakeover.admin.core.ui.theme.DangerCancel
import com.uniquemakeover.admin.core.ui.theme.DangerSoft
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.SurfaceCard
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.TextPrimary

private const val PHONE_COLUMNS = 3
private const val TABLET_MEDIUM_COLUMNS = 4
private const val TABLET_LARGE_COLUMNS = 6

private const val MEDIUM_SCREEN_WIDTH_DP = 600
private const val EXPANDED_SCREEN_WIDTH_DP = 840
private const val DISABLED_ALPHA = 0.38f
private const val SLOT_CHIP_HEIGHT_DP = 52
private const val CORNER_RADIUS_DP = 10

@Composable
fun SlotGrid(
    slots: List<Slot>,
    selectedSlot: Slot?,
    onSlotSelected: (Slot) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val columnCount = when {
        configuration.screenWidthDp >= EXPANDED_SCREEN_WIDTH_DP -> TABLET_LARGE_COLUMNS
        configuration.screenWidthDp >= MEDIUM_SCREEN_WIDTH_DP -> TABLET_MEDIUM_COLUMNS
        else -> PHONE_COLUMNS
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(slots, key = { it.time24 }) { slot ->
            SlotChip(
                slot = slot,
                isSelected = selectedSlot?.time24 == slot.time24,
                onSlotSelected = onSlotSelected
            )
        }
    }
}

@Composable
fun SlotChip(
    slot: Slot,
    isSelected: Boolean,
    onSlotSelected: (Slot) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(CORNER_RADIUS_DP.dp)
    val animatedBg by animateColorAsState(
        targetValue = when {
            isSelected -> LuxuryGold
            slot.isAvailable -> SurfaceCard
            else -> SurfaceCard.copy(alpha = 0.5f)
        },
        label = "slotChipBg"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SLOT_CHIP_HEIGHT_DP.dp)
            .clip(shape)
            .background(animatedBg)
            .border(
                width = 1.dp,
                color = if (isSelected) LuxuryGold else BorderLine,
                shape = shape
            )
            .alpha(if (slot.isAvailable) 1f else DISABLED_ALPHA)
            .clickable(enabled = slot.isAvailable) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSlotSelected(slot)
            }
            .semantics {
                role = Role.RadioButton
                contentDescription = "${slot.time12} - ${if (slot.isAvailable) "Available" else "Full"}"
            },
        contentAlignment = Alignment.Center
    ) {
        if (slot.isAvailable) {
            AvailableSlotContent(slot = slot, isSelected = isSelected)
        } else {
            UnavailableSlotContent(slot = slot)
        }
    }
}

@Composable
private fun AvailableSlotContent(
    slot: Slot,
    isSelected: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = slot.time12,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else TextPrimary
            )
        )
    }
}

@Composable
private fun UnavailableSlotContent(
    slot: Slot
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = slot.time12,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Normal,
                color = TextMuted
            )
        )
        when (slot.reason) {
            UnavailableReason.ALREADY_BOOKED -> {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .background(DangerSoft, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "Full",
                        color = DangerCancel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            UnavailableReason.PAST_TIME -> {
                Text(
                    text = "Past",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            null -> Unit
        }
    }
}
