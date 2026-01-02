package com.example.geo_alarm.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.geo_alarm.navigation.MainRoutes
import com.example.geo_alarm.ui.components.CustomBottomBar
import com.example.geo_alarm.ui.components.NavTab
import com.example.geo_alarm.ui.viewmodel.HomeViewModel

@Composable
fun MainScreen(
    viewModel: HomeViewModel,
    onAddAlarm: () -> Unit,
    onAlarmClick: (String) -> Unit,
    onNavigateToBatteryOptimization: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    // Determine if we are on a top-level tab
    val isHome = navBackStackEntry?.destination?.hasRoute<MainRoutes.Home>() == true
    val isSettings = navBackStackEntry?.destination?.hasRoute<MainRoutes.Settings>() == true

    // Animation States
    val alignmentBias by animateFloatAsState(
        targetValue = if (isSettings) 0f else -1f, label = "bias"
    )
    val startPadding by animateDpAsState(
        targetValue = if (isSettings) 0.dp else 16.dp, label = "padding"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController, startDestination = MainRoutes.Home
        ) {
            composable<MainRoutes.Home>(
                enterTransition = { fadeIn(animationSpec = tween(300)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) }) {
                HomeScreen(
                    viewModel = viewModel,
                    onAddAlarm = onAddAlarm,
                    onAlarmClick = { alarm -> onAlarmClick(alarm.id) },
                    onNavigateToBatteryOptimization = onNavigateToBatteryOptimization
                )
            }

            composable<MainRoutes.Settings>(
                enterTransition = { fadeIn(animationSpec = tween(300)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) }
            ) {
                // Obtain the Application context
                val context = androidx.compose.ui.platform.LocalContext.current
                val app = context.applicationContext as com.example.geo_alarm.GeoAlarmApplication
                
                // Create the factory with dependencies
                val factory = com.example.geo_alarm.ui.viewmodel.ViewModelFactory(app, app.repository, app.settingsRepository)
                
                // Get the SettingsViewModel
                val settingsViewModel: com.example.geo_alarm.ui.viewmodel.SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
                
                SettingsScreen(
                    viewModel = settingsViewModel
                )
            }
        }

        CustomBottomBar(
            currentTab = if (isSettings) NavTab.SETTINGS else NavTab.HOME,
            onHomeClick = {
                if (!isHome) {
                    navController.navigate(MainRoutes.Home) {
                        popUpTo(navController.graph.id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            onSettingsClick = {
                if (!isSettings) {
                    navController.navigate(MainRoutes.Settings) {
                        popUpTo(navController.graph.id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            modifier = Modifier
                .align(BiasAlignment(alignmentBias, 1f))
                .padding(start = startPadding, bottom = 32.dp)
        )
    }
}
