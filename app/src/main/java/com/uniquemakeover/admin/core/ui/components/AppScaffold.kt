package com.uniquemakeover.admin.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import com.uniquemakeover.admin.core.ui.theme.BlushSurface
import com.uniquemakeover.admin.core.ui.theme.BrandPrimary
import com.uniquemakeover.admin.core.ui.theme.SurfaceCard
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.core.ui.theme.WarmBackground
import com.uniquemakeover.admin.navigation.Routes

private const val MEDIUM_WIDTH_BREAKPOINT_DP = 600

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val MainNavItems = listOf(
    NavItem(Routes.Dashboard, "Dashboard", Icons.Outlined.Dashboard),
    NavItem(Routes.Appointments, "Appointments", Icons.Outlined.Event),
    NavItem(Routes.Services, "Services", Icons.Outlined.ContentCut),
    NavItem(Routes.Clients, "Clients", Icons.Outlined.People),
    NavItem(Routes.Reminders, "Reminders", Icons.Outlined.Notifications),
    NavItem(Routes.Settings, "Settings", Icons.Outlined.Settings)
)

@Composable
fun AppScaffold(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    showNavigation: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= MEDIUM_WIDTH_BREAKPOINT_DP

    if (!showNavigation) {
        Box(modifier = modifier.fillMaxSize().background(WarmBackground)) {
            content()
        }
        return
    }

    if (isTablet) {
        TabletNavLayout(
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            modifier = modifier,
            content = content
        )
    } else {
        PhoneNavLayout(
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            modifier = modifier,
            content = content
        )
    }
}

@Composable
private fun TabletNavLayout(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(WarmBackground)
    ) {
        AppNavigationRail(
            currentRoute = currentRoute,
            onNavigate = onNavigate
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            content()
        }
    }
}

@Composable
private fun AppNavigationRail(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationRail(
        containerColor = SurfaceCard,
        contentColor = TextMuted,
        modifier = Modifier.fillMaxHeight()
    ) {
        MainNavItems.forEach { item ->
            NavigationRailItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                label = { Text(text = item.label) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = BrandPrimary,
                    selectedTextColor = BrandPrimary,
                    indicatorColor = BlushSurface,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                )
            )
        }
    }
}

@Composable
private fun PhoneNavLayout(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WarmBackground,
        bottomBar = {
            AppBottomNavigationBar(
                currentRoute = currentRoute,
                onNavigate = onNavigate
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            content()
        }
    }
}

@Composable
private fun AppBottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(containerColor = SurfaceCard) {
        MainNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                label = { Text(text = item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrandPrimary,
                    selectedTextColor = BrandPrimary,
                    indicatorColor = BlushSurface,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                )
            )
        }
    }
}
