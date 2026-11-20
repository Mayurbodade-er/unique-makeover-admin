package com.uniquemakeover.admin.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.uniquemakeover.admin.core.ui.components.AppScaffold
import com.uniquemakeover.admin.feature.appointments.presentation.AppointmentsRoute
import com.uniquemakeover.admin.feature.auth.presentation.PinLockRoute
import com.uniquemakeover.admin.feature.clients.presentation.detail.ClientDetailRoute
import com.uniquemakeover.admin.feature.clients.presentation.list.ClientsRoute
import com.uniquemakeover.admin.feature.dashboard.presentation.DashboardRoute
import com.uniquemakeover.admin.feature.reminders.presentation.RemindersRoute
import com.uniquemakeover.admin.feature.services.presentation.ServicesRoute
import com.uniquemakeover.admin.feature.settings.presentation.SettingsRoute
import com.uniquemakeover.admin.feature.upi.presentation.UpiQrRoute

@Composable
fun UniqueMakeoverNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.PinLock
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: startDestination
    val showNavigation = currentRoute != Routes.PinLock &&
        currentRoute != Routes.ClientDetail &&
        currentRoute != Routes.UpiQr &&
        !currentRoute.startsWith("upi_qr")

    AppScaffold(
        currentRoute = currentRoute,
        onNavigate = { targetRoute ->
            if (currentRoute != targetRoute) {
                navController.navigate(targetRoute) {
                    popUpTo(Routes.Dashboard) {
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
        coreNavDestinations(navController)
        managementNavDestinations(navController)
    }
}

private fun NavGraphBuilder.coreNavDestinations(navController: NavHostController) {
    composable(Routes.PinLock) {
        PinLockRoute(
            viewModel = hiltViewModel(),
            onAuthenticated = {
                navController.navigate(Routes.Dashboard) {
                    popUpTo(Routes.PinLock) { inclusive = true }
                }
            }
        )
    }

    composable(Routes.Dashboard) {
        DashboardRoute(
            viewModel = hiltViewModel(),
            onNavigateToUpiQr = { amount, rate, paid, clientName ->
                navController.navigate(Routes.upiQr(amount, rate, paid, clientName))
            }
        )
    }

    composable(Routes.Appointments) {
        AppointmentsRoute(viewModel = hiltViewModel())
    }

    composable(Routes.Services) {
        ServicesRoute(viewModel = hiltViewModel())
    }
}

private fun NavGraphBuilder.managementNavDestinations(navController: NavHostController) {
    composable(Routes.Clients) {
        ClientsRoute(
            viewModel = hiltViewModel(),
            onClientClick = { phone ->
                navController.navigate(Routes.clientDetail(phone))
            }
        )
    }

    composable(Routes.ClientDetail) {
        ClientDetailRoute(
            viewModel = hiltViewModel(),
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(Routes.Reminders) {
        RemindersRoute(viewModel = hiltViewModel())
    }

    composable(Routes.Settings) {
        SettingsRoute(viewModel = hiltViewModel())
    }

    upiNavDestination(navController)
}

private fun NavGraphBuilder.upiNavDestination(navController: NavHostController) {
    composable(
        route = Routes.UpiQr,
        arguments = listOf(
            navArgument("amount") {
                type = NavType.IntType
                defaultValue = 0
            },
            navArgument("rate") {
                type = NavType.IntType
                defaultValue = 0
            },
            navArgument("paid") {
                type = NavType.IntType
                defaultValue = 0
            },
            navArgument("clientName") {
                type = NavType.StringType
                defaultValue = ""
            }
        )
    ) {
        UpiQrRoute(
            viewModel = hiltViewModel(),
            onNavigateBack = { navController.popBackStack() },
            onConfigureUpiClick = { navController.navigate(Routes.Settings) }
        )
    }
}
