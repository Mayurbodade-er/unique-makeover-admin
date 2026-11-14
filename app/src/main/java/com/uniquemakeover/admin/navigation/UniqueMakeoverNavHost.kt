package com.uniquemakeover.admin.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.uniquemakeover.admin.feature.auth.presentation.PinLockRoute
import com.uniquemakeover.admin.feature.services.presentation.ServicesRoute

@Composable
fun UniqueMakeoverNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.PinLock
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

        composable(Routes.Services) {
            ServicesRoute(
                viewModel = hiltViewModel()
            )
        }
    }
}
