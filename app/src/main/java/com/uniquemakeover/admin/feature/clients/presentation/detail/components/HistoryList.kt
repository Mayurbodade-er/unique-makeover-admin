package com.uniquemakeover.admin.feature.clients.presentation.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.uniquemakeover.admin.core.domain.model.AppointmentStatus
import com.uniquemakeover.admin.core.ui.theme.BlushSurface
import com.uniquemakeover.admin.core.ui.theme.BorderLine
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.GoldSoft
import com.uniquemakeover.admin.core.ui.theme.SuccessOk
import com.uniquemakeover.admin.core.ui.theme.SuccessSoft
import com.uniquemakeover.admin.core.ui.theme.SurfaceCard
import com.uniquemakeover.admin.core.ui.theme.TextHeading
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.WarningDue
import com.uniquemakeover.admin.core.ui.theme.WarningSoft
import com.uniquemakeover.admin.feature.clients.domain.model.ClientHistoryItem

private const val NEUTRAL_BG_HEX = 0xFFEEEEEE
private val NeutralBadgeBg = Color(NEUTRAL_BG_HEX)

@Composable
fun HistoryList(
    historyItems: List<ClientHistoryItem>,
    modifier: Modifier = Modifier
) {
    if (historyItems.isEmpty()) {
        EmptyHistoryView(modifier = modifier)
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            historyItems.forEach { item ->
                HistoryItemCard(item = item)
            }
        }
    }
}

@Composable
private fun HistoryItemCard(item: ClientHistoryItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLine))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            HistoryCardHeader(date = item.date, time = item.time, status = item.status)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.formattedServices.ifBlank { "General Beauty Service" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextHeading
            )

            if (item.staff.isNotBlank()) {
                Text(
                    text = "Attended by ${item.staff}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            HistoryFinancialsRow(
                rate = item.rate,
                paid = item.paid,
                balanceDue = item.balanceDue,
                isCancelled = item.status == AppointmentStatus.CANCEL
            )
        }
    }
}

@Composable
private fun HistoryCardHeader(
    date: String,
    time: String,
    status: AppointmentStatus
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$date • $time",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = BrandPrimary
        )

        StatusBadge(status = status)
    }
}

@Composable
private fun StatusBadge(status: AppointmentStatus) {
    val (label, textCol, bgCol) = when (status) {
        AppointmentStatus.DONE -> Triple("Done ✓", SuccessOk, SuccessSoft)
        AppointmentStatus.BOOKED -> Triple("Booked", BrandPrimary, BlushSurface)
        AppointmentStatus.CANCEL -> Triple("Cancelled", TextMuted, NeutralBadgeBg)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgCol)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textCol
        )
    }
}

@Composable
private fun HistoryFinancialsRow(
    rate: Int,
    paid: Int,
    balanceDue: Int,
    isCancelled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Rate: ₹ $rate",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = TextHeading
            )
            if (!isCancelled) {
                Text(
                    text = "Paid: ₹ $paid",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        if (balanceDue > 0 && !isCancelled) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(WarningSoft)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Due: ₹ $balanceDue",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = WarningDue
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.EventNote,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = TextMuted.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No visit history recorded yet",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
    }
}
