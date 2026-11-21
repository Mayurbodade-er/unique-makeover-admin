package com.uniquemakeover.admin.feature.dashboard.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.DangerCancel
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.SurfaceCard
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.TextPrimary
import com.uniquemakeover.admin.core.ui.theme.WarningDue
import com.uniquemakeover.admin.feature.dashboard.domain.model.AlertBanner

private const val ACCENT_STRIP_WIDTH_DP = 6
private const val ICON_BOX_SIZE_DP = 40
private const val ICON_SIZE_DP = 22

private data class BannerVisuals(
    val title: String,
    val description: String,
    val actionText: String,
    val accentColor: Color,
    val icon: ImageVector
)

@Composable
fun AlertBannerCard(
    banner: AlertBanner,
    onActionClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visuals = resolveBannerVisuals(banner)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, visuals.accentColor.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(ACCENT_STRIP_WIDTH_DP.dp)
                    .fillMaxHeight()
                    .background(visuals.accentColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                BannerIconBox(icon = visuals.icon, tint = visuals.accentColor)
                Spacer(modifier = Modifier.width(12.dp))
                BannerContent(
                    visuals = visuals,
                    onActionClick = onActionClick,
                    onDismissClick = onDismissClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BannerIconBox(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(ICON_BOX_SIZE_DP.dp)
            .background(tint.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(ICON_SIZE_DP.dp)
        )
    }
}

@Composable
private fun BannerContent(
    visuals: BannerVisuals,
    onActionClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = visuals.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismissClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Dismiss",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = visuals.description,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onActionClick,
            colors = ButtonDefaults.textButtonColors(contentColor = visuals.accentColor)
        ) {
            Text(text = visuals.actionText, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun resolveBannerVisuals(banner: AlertBanner): BannerVisuals {
    return when (banner) {
        is AlertBanner.BackupOverdueMissing -> BannerVisuals(
            title = "Backup Overdue!",
            description = "No database backup found. Protect client records.",
            actionText = "Backup Now →",
            accentColor = DangerCancel,
            icon = Icons.Outlined.CloudOff
        )
        is AlertBanner.BackupOverdue -> BannerVisuals(
            title = "Backup Overdue!",
            description = "Last backup was ${banner.daysSinceLast} days ago. Protect client records.",
            actionText = "Backup Now →",
            accentColor = DangerCancel,
            icon = Icons.Outlined.CloudOff
        )
        is AlertBanner.RemindersPending -> BannerVisuals(
            title = "Reminders Pending",
            description = "${banner.count} client reminder(s) pending for tomorrow.",
            actionText = "Send Now →",
            accentColor = LuxuryGold,
            icon = Icons.Outlined.NotificationsActive
        )
        is AlertBanner.UpiNotConfigured -> BannerVisuals(
            title = "UPI Not Configured",
            description = "Merchant UPI ID missing. Offline QR payments disabled.",
            actionText = "Setup UPI →",
            accentColor = WarningDue,
            icon = Icons.Outlined.QrCodeScanner
        )
        is AlertBanner.WaNotConfigured -> BannerVisuals(
            title = "WhatsApp Not Configured",
            description = "WhatsApp number not configured. Tap to setup.",
            actionText = "Setup WhatsApp →",
            accentColor = WarningDue,
            icon = Icons.Outlined.Forum
        )
        is AlertBanner.OverdueAppointments -> BannerVisuals(
            title = "Overdue Appointments",
            description = "${banner.count} overdue appointment(s) pending settlement.",
            actionText = "Review Now →",
            accentColor = BrandPrimary,
            icon = Icons.Outlined.PendingActions
        )
    }
}
