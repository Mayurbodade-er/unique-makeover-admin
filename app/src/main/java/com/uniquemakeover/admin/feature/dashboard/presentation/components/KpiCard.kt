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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import com.uniquemakeover.admin.core.ui.theme.BorderLine
import com.uniquemakeover.admin.core.ui.theme.SurfaceCard
import com.uniquemakeover.admin.core.ui.theme.TextMuted

private const val CARD_CORNER_RADIUS_DP = 14
private const val ICON_CONTAINER_SIZE_DP = 36
private const val ICON_SIZE_DP = 20

data class KpiCardData(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val iconTint: Color,
    val iconBgColor: Color,
    val valueColor: Color
)

@Composable
fun KpiCard(
    data: KpiCardData,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(CARD_CORNER_RADIUS_DP.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceCard)
            .border(1.dp, BorderLine, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = data.title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextMuted
            )
            Box(
                modifier = Modifier
                    .size(ICON_CONTAINER_SIZE_DP.dp)
                    .clip(CircleShape)
                    .background(data.iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = data.icon,
                    contentDescription = null,
                    tint = data.iconTint,
                    modifier = Modifier.size(ICON_SIZE_DP.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = data.value,
            color = data.valueColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
