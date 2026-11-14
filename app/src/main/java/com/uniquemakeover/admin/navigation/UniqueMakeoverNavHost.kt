package com.uniquemakeover.admin.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.uniquemakeover.admin.core.ui.components.AppScaffold
import com.uniquemakeover.admin.core.ui.theme.TextMuted
import com.uniquemakeover.admin.feature.appointments.presentation.AppointmentsRoute
import com.uniquemakeover.admin.feature.auth.presentation.PinLockRoute
import com.uniquemakeover.admin.feature.clients.presentation.list.ClientsRoute
import com.uniquemakeover.admin.feature.dashboard.presentation.DashboardRoute
import com.uniquemakeover.admin.feature.services.presentation.ServicesRoute
import com.uniquemakeover.admin.feature.settings.presentation.SettingsRoute

@Composable
fun UniqueMakeoverNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.PinLock
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: startDestination
    val showNavigation = currentRoute != Routes.PinLock

    AppScaffold(
        currentRoute = currentRoute,
        onNavigate = { targetRoute ->
            if (currentRoute != targetRoute) {
                navController.navigate(targetRoute) {
                    popUpTo(Routes.Services) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        showNavigation = showNavigation,
        modifier = modifier
    ) {
        UniqueMakeoverNavGraph(
            navController = navController,
            startDestination = startDestination
        )
    }
}

@Composable
private fun UniqueMakeoverNavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.PinLock) {
            PinLockRoute(
                viewModel = hiltViewModel(),
                onAuthenticated = {
                    navController.navigate(Routes.Services) {
                        popUpTo(Routes.PinLock) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Dashboard) {
            DashboardRoute(
                viewModel = hiltViewModel()
            )
        }

        composable(Routes.Appointments) {
            AppointmentsRoute(
                viewModel = hiltViewModel()
            )
        }

        composable(Routes.Services) {
            ServicesRoute(
                viewModel = hiltViewModel()
            )
        }

        composable(Routes.Clients) {
            ClientsRoute(
                viewModel = hiltViewModel()
            )
        }

        composable(Routes.Reminders) {
            PlaceholderScreen(title = "Reminders")
        }

        composable(Routes.Settings) {
            SettingsRoute(
                viewModel = hiltViewModel()
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$title (Coming Soon)",
            style = MaterialTheme.typography.titleMedium,
            color = TextMuted
        )
    }
}
