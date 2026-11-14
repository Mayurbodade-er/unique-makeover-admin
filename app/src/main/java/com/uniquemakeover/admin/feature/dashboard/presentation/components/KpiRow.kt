package com.uniquemakeover.admin.feature.dashboard.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.uniquemakeover.admin.core.ui.theme.BlushSurface
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.DangerCancel
import com.uniquemakeover.admin.core.ui.theme.DangerSoft
import com.uniquemakeover.admin.core.ui.theme.GoldSoft
import com.uniquemakeover.admin.core.ui.theme.LuxuryGold
import com.uniquemakeover.admin.core.ui.theme.SuccessOk
import com.uniquemakeover.admin.core.ui.theme.SuccessSoft
import com.uniquemakeover.admin.core.ui.theme.TextPrimary
import com.uniquemakeover.admin.core.ui.theme.WarningDue
import com.uniquemakeover.admin.core.ui.theme.WarningSoft
import com.uniquemakeover.admin.core.util.CurrencyFormatter
import com.uniquemakeover.admin.feature.dashboard.domain.model.DashboardKpis

@Composable
fun KpiRow(
    kpis: DashboardKpis,
    modifier: Modifier = Modifier
) {
    val cards = buildKpiCards(kpis)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        cards.chunked(2).forEach { rowPair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowPair.forEach { cardData ->
                    KpiCard(
                        data = cardData,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowPair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun buildKpiCards(kpis: DashboardKpis): List<KpiCardData> = listOf(
    KpiCardData(
        title = "Scheduled Volume",
        value = CurrencyFormatter.formatInr(kpis.scheduledVolume),
        icon = Icons.Outlined.Payments,
        iconTint = LuxuryGold,
        iconBgColor = GoldSoft,
        valueColor = TextPrimary
    ),
    KpiCardData(
        title = "Collected Revenue",
        value = CurrencyFormatter.formatInr(kpis.collectedRevenue),
        icon = Icons.Outlined.AccountBalanceWallet,
        iconTint = SuccessOk,
        iconBgColor = SuccessSoft,
        valueColor = SuccessOk
    ),
    KpiCardData(
        title = "Outstanding Due",
        value = CurrencyFormatter.formatInr(kpis.balanceOutstanding),
        icon = Icons.Outlined.HourglassEmpty,
        iconTint = WarningDue,
        iconBgColor = WarningSoft,
        valueColor = WarningDue
    ),
    KpiCardData(
        title = "Booked",
        value = kpis.bookedCount.toString(),
        icon = Icons.Outlined.DateRange,
        iconTint = BrandPrimary,
        iconBgColor = BlushSurface,
        valueColor = BrandPrimary
    ),
    KpiCardData(
        title = "Completed",
        value = kpis.completedCount.toString(),
        icon = Icons.Outlined.CheckCircle,
        iconTint = SuccessOk,
        iconBgColor = SuccessSoft,
        valueColor = SuccessOk
    ),
    KpiCardData(
        title = "Cancelled",
        value = kpis.cancelledCount.toString(),
        icon = Icons.Outlined.Cancel,
        iconTint = DangerCancel,
        iconBgColor = DangerSoft,
        valueColor = DangerCancel
    )
)
