package com.uniquemakeover.admin.feature.clients.presentation.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uniquemakeover.admin.core.ui.theme.BlushSurface
import com.uniquemakeover.admin.core.ui.theme.BorderLine
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.GoldSoft
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.SuccessOk
import com.uniquemakeover.admin.core.ui.theme.SuccessSoft
import com.uniquemakeover.admin.core.ui.theme.SurfaceCard
import com.uniquemakeover.admin.core.ui.theme.TextHeading
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.feature.clients.domain.model.ClientProfile

private const val NEUTRAL_BG_HEX = 0xFFEEEEEE
private val NeutralBadgeBg = Color(NEUTRAL_BG_HEX)

@Composable
fun ClientRow(
    profile: ClientProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLine))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ClientAvatar(name = profile.name)

            Spacer(modifier = Modifier.width(12.dp))

            ClientInfoColumn(
                profile = profile,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            ClientMetricsColumn(profile = profile)
        }
    }
}

@Composable
private fun ClientAvatar(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(GoldSoft)
            .border(1.5.dp, LuxuryGold, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = extractInitials(name),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = BrandPrimary
        )
    }
}

@Composable
private fun ClientInfoColumn(
    profile: ClientProfile,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = profile.name.ifBlank { "Client" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextHeading,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "+91 ${profile.phone}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
        if (profile.lastServices.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = profile.lastServices,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ClientMetricsColumn(
    profile: ClientProfile,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (profile.totalSpend > 0) SuccessSoft else GoldSoft)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "₹ ${profile.totalSpend}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (profile.totalSpend > 0) SuccessOk else TextMuted
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (profile.totalVisits > 0) BlushSurface else NeutralBadgeBg)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            val visitLabel = if (profile.totalVisits == 1) "1 Visit" else "${profile.totalVisits} Visits"
            Text(
                text = visitLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (profile.totalVisits > 0) BrandPrimary else TextMuted
            )
        }
    }
}

private fun extractInitials(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "?"
    val parts = trimmed.split(" ").filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
        parts[0].length >= 2 -> parts[0].take(2).uppercase()
        else -> parts[0].take(1).uppercase()
    }
}
