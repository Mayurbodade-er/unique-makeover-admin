package com.uniquemakeover.admin.feature.clients.presentation.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uniquemakeover.admin.core.ui.theme.BorderLine
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.GoldSoft
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.SuccessOk
import com.uniquemakeover.admin.core.ui.theme.SuccessSoft
import com.uniquemakeover.admin.core.ui.theme.SurfaceCard
import com.uniquemakeover.admin.core.ui.theme.TextHeading
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.WhatsAppDarkText
import com.uniquemakeover.admin.core.ui.theme.WhatsAppGreen
import com.uniquemakeover.admin.feature.clients.presentation.detail.ClientDetailActions
import com.uniquemakeover.admin.feature.clients.presentation.detail.ClientDetailUiState

@Composable
fun ClientMetricsHeader(
    uiState: ClientDetailUiState,
    actions: ClientDetailActions,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLine))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            ClientIdentityRow(
                clientName = uiState.clientName,
                phone = uiState.phone,
                onQuickRebookClick = actions.onQuickRebookClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            ClientKpiCardsRow(
                totalSpend = uiState.totalSpend,
                totalVisits = uiState.totalVisits
            )

            Spacer(modifier = Modifier.height(16.dp))

            CommunicationActionsRow(
                onCallClick = actions.onCallClick,
                onWhatsAppClick = actions.onWhatsAppClick
            )
        }
    }
}

@Composable
private fun ClientIdentityRow(
    clientName: String,
    phone: String,
    onQuickRebookClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = clientName.ifBlank { "Client Profile" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextHeading
            )
            Text(
                text = "+91 $phone",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }

        Button(
            onClick = onQuickRebookClick,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold),
            modifier = Modifier.defaultMinSize(minHeight = 44.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Bolt,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Rebook", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ClientKpiCardsRow(totalSpend: Int, totalVisits: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricTile(
            label = "Lifetime Spend",
            value = "₹ $totalSpend",
            valueColor = SuccessOk,
            containerColor = SuccessSoft,
            modifier = Modifier.weight(1f)
        )
        MetricTile(
            label = "Total Visits",
            value = "$totalVisits ${if (totalVisits == 1) "Visit" else "Visits"}",
            valueColor = BrandPrimary,
            containerColor = GoldSoft,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    containerColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .padding(vertical = 12.dp, horizontal = 14.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

@Composable
private fun CommunicationActionsRow(
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onCallClick,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
        ) {
            Icon(Icons.Outlined.Phone, contentDescription = null, tint = BrandPrimary)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Call Client", color = BrandPrimary, fontWeight = FontWeight.Medium)
        }

        Button(
            onClick = onWhatsAppClick,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WhatsAppGreen,
                contentColor = WhatsAppDarkText
            ),
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
        ) {
            Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, tint = WhatsAppDarkText)
            Spacer(modifier = Modifier.width(6.dp))
            Text("WhatsApp", fontWeight = FontWeight.Bold)
        }
    }
}
