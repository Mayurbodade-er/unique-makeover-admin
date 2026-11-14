package com.uniquemakeover.admin.feature.appointments.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.uniquemakeover.admin.feature.appointments.domain.model.Appointment

private const val CARD_CORNER_RADIUS_DP = 14
private const val CHIP_CORNER_RADIUS_DP = 8

@Composable
fun AppointmentCard(
    appointment: Appointment,
    onClick: (Appointment) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(CARD_CORNER_RADIUS_DP.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceCard)
            .border(1.dp, BorderLine, shape)
            .clickable { onClick(appointment) }
            .padding(16.dp)
    ) {
        AppointmentCardHeader(appointment = appointment)
        Spacer(modifier = Modifier.height(10.dp))
        AppointmentCardClient(appointment = appointment)
        Spacer(modifier = Modifier.height(12.dp))
        AppointmentCardFinancials(appointment = appointment)
    }
}

@Composable
private fun AppointmentCardHeader(appointment: Appointment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${appointment.date} · ${appointment.time}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = BrandPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            StaffPill(staffName = appointment.staff)
        }
        StatusBadge(status = appointment.status)
    }
}

@Composable
private fun StaffPill(staffName: String) {
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
                modifier = Modifier.padding(end = 4.dp).height(12.dp)
            )
            Text(
                text = staffName.ifBlank { "Staff" },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = LuxuryGold
            )
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
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
private fun AppointmentCardClient(appointment: Appointment) {
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
                modifier = Modifier.padding(end = 4.dp).height(14.dp)
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
private fun AppointmentCardFinancials(appointment: Appointment) {
    val balanceDue = (appointment.rate - appointment.paid).coerceAtLeast(0)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Rate: ₹${appointment.rate}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Paid: ₹${appointment.paid}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }

        Box(
            modifier = Modifier
                .background(
                    if (balanceDue > 0) WarningSoft else SuccessSoft,
                    RoundedCornerShape(CHIP_CORNER_RADIUS_DP.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (balanceDue > 0) "Due: ₹$balanceDue" else "Paid in Full",
                color = if (balanceDue > 0) WarningDue else SuccessOk,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
