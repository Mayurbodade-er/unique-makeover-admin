package com.uniquemakeover.admin.feature.dashboard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uniquemakeover.admin.core.ui.theme.BlushSurface
import com.uniquemakeover.admin.core.ui.theme.BorderLine
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.DangerCancel
import com.uniquemakeover.admin.core.ui.theme.DangerSoft
import com.uniquemakeover.admin.core.ui.theme.GoldSoft
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.SuccessOk
import com.uniquemakeover.admin.core.ui.theme.SuccessSoft
import com.uniquemakeover.admin.core.ui.theme.SurfaceCard
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.TextPrimary
import com.uniquemakeover.admin.core.ui.theme.WarningDue
import com.uniquemakeover.admin.core.ui.theme.WarningSoft
import com.uniquemakeover.admin.core.ui.theme.WhatsAppGreen
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment
import com.uniquemakeover.admin.feature.dashboard.domain.model.ScheduleItem

private const val CARD_CORNER_RADIUS_DP = 14
private const val CHIP_CORNER_RADIUS_DP = 8
private const val ACTION_ICON_SIZE_DP = 22

data class ScheduleCardActions(
    val onMarkDone: (Appointment) -> Unit = {},
    val onWhatsApp: (Appointment) -> Unit = {},
    val onRecordPayment: (Appointment) -> Unit = {},
    val onEdit: (Appointment) -> Unit = {},
    val onCancel: (Appointment) -> Unit = {},
    val onDelete: (Appointment) -> Unit = {}
)

@Composable
fun ScheduleRowCard(
    item: ScheduleItem,
    actions: ScheduleCardActions = ScheduleCardActions(),
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(CARD_CORNER_RADIUS_DP.dp)
    val appointment = item.appointment

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceCard)
            .border(1.dp, BorderLine, shape)
            .padding(14.dp)
    ) {
        ScheduleCardHeader(item = item)
        Spacer(modifier = Modifier.height(10.dp))
        ScheduleCardClientInfo(appointment = appointment)
        Spacer(modifier = Modifier.height(12.dp))
        ScheduleCardFinancials(appointment = appointment)
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = BorderLine.copy(alpha = 0.6f), thickness = 1.dp)
        ScheduleActionStrip(appointment = appointment, actions = actions)
    }
}

@Composable
private fun ScheduleCardHeader(item: ScheduleItem) {
    val appointment = item.appointment
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = appointment.time,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (item.isPastTime && appointment.status == "booked") TextMuted else BrandPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(GoldSoft, RoundedCornerShape(CHIP_CORNER_RADIUS_DP.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Face,
                        contentDescription = null,
                        tint = LuxuryGold,
                        modifier = Modifier.size(12.dp).padding(end = 2.dp)
                    )
                    Text(
                        text = appointment.staff.ifBlank { "Staff" },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = LuxuryGold
                    )
                }
            }
        }
        ScheduleStatusBadge(status = appointment.status)
    }
}

@Composable
private fun ScheduleStatusBadge(status: String) {
    val isDone = status.lowercase() == "done"
    val isCancel = status.lowercase() in listOf("cancel", "cancelled")
    val (bg, textColor, label) = when {
        isDone -> Triple(SuccessSoft, SuccessOk, "Completed")
        isCancel -> Triple(DangerSoft, DangerCancel, "Cancelled")
        else -> Triple(BlushSurface, BrandPrimary, "Booked")
    }

    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(CHIP_CORNER_RADIUS_DP.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun ScheduleCardClientInfo(appointment: Appointment) {
    Column {
        Text(
            text = appointment.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Phone,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(14.dp).padding(end = 4.dp)
            )
            Text(
                text = "+91 ${appointment.phone}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        if (appointment.serviceSnapshot.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = appointment.serviceSnapshot,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun ScheduleCardFinancials(appointment: Appointment) {
    val balanceDue = (appointment.rate - appointment.paid).coerceAtLeast(0)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Rate: ₹${appointment.rate}  ·  Paid: ₹${appointment.paid}",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = TextMuted
        )
        Box(
            modifier = Modifier
                .background(
                    if (balanceDue > 0) WarningSoft else SuccessSoft,
                    RoundedCornerShape(CHIP_CORNER_RADIUS_DP.dp)
                )
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = if (balanceDue > 0) "Due: ₹$balanceDue" else "Paid",
                color = if (balanceDue > 0) WarningDue else SuccessOk,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ScheduleActionStrip(
    appointment: Appointment,
    actions: ScheduleCardActions
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionIconButton(Icons.Outlined.CheckCircle, "Mark Done", SuccessOk) {
            actions.onMarkDone(appointment)
        }
        ActionIconButton(Icons.AutoMirrored.Outlined.Send, "WhatsApp", WhatsAppGreen) {
            actions.onWhatsApp(appointment)
        }
        ActionIconButton(Icons.Outlined.Payments, "Record Payment", BrandPrimary) {
            actions.onRecordPayment(appointment)
        }
        ActionIconButton(Icons.Outlined.Edit, "Edit", TextMuted) {
            actions.onEdit(appointment)
        }
        ActionIconButton(Icons.Outlined.Cancel, "Cancel", DangerCancel) {
            actions.onCancel(appointment)
        }
        ActionIconButton(Icons.Outlined.Delete, "Delete", TextMuted) {
            actions.onDelete(appointment)
        }
    }
}

@Composable
private fun ActionIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(ACTION_ICON_SIZE_DP.dp)
        )
    }
}
