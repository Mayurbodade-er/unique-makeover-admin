package com.uniquemakeover.admin.feature.appointments.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uniquemakeover.admin.core.ui.theme.BlushSurface
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.TextPrimary
import com.uniquemakeover.admin.core.ui.theme.WarmBackground
import com.uniquemakeover.admin.feature.appointments.domain.model.FilterTab
import com.uniquemakeover.admin.feature.appointments.presentation.TabCounts

private const val BADGE_RADIUS_DP = 10
private const val TAB_MIN_HEIGHT_DP = 48

@Composable
fun FilterTabs(
    selectedTab: FilterTab,
    tabCounts: TabCounts,
    onTabSelected: (FilterTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = FilterTab.entries
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = WarmBackground,
        contentColor = BrandPrimary,
        edgePadding = 16.dp,
        indicator = { tabPositions ->
            if (selectedIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    color = BrandPrimary,
                    height = 3.dp
                )
            }
        },
        divider = {},
        modifier = modifier.fillMaxWidth()
    ) {
        tabs.forEach { tab ->
            FilterTabItem(
                tab = tab,
                isSelected = tab == selectedTab,
                count = tabCounts.countFor(tab),
                onTabSelected = onTabSelected
            )
        }
    }
}

@Composable
private fun FilterTabItem(
    tab: FilterTab,
    isSelected: Boolean,
    count: Int,
    onTabSelected: (FilterTab) -> Unit
) {
    Tab(
        selected = isSelected,
        onClick = { onTabSelected(tab) },
        modifier = Modifier.height(TAB_MIN_HEIGHT_DP.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = tab.displayTitle(),
                color = if (isSelected) BrandPrimary else TextMuted,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            TabBadge(count = count, isSelected = isSelected)
        }
    }
}

@Composable
private fun TabBadge(count: Int, isSelected: Boolean) {
    Box(
        modifier = Modifier
            .background(
                color = if (isSelected) BrandPrimary else BlushSurface,
                shape = RoundedCornerShape(BADGE_RADIUS_DP.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = count.toString(),
            color = if (isSelected) Color.White else TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

private fun FilterTab.displayTitle(): String = when (this) {
    FilterTab.ALL -> "All"
    FilterTab.UPCOMING -> "Upcoming"
    FilterTab.COMPLETED -> "Completed"
    FilterTab.CANCELLED -> "Cancelled"
}

private fun TabCounts.countFor(tab: FilterTab): Int = when (tab) {
    FilterTab.ALL -> all
    FilterTab.UPCOMING -> upcoming
    FilterTab.COMPLETED -> completed
    FilterTab.CANCELLED -> cancelled
}
