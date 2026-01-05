package com.github.jimmy90109.geoalarm.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.jimmy90109.geoalarm.GeoAlarmApplication
import com.github.jimmy90109.geoalarm.navigation.MainRoutes
import com.github.jimmy90109.geoalarm.ui.components.BottomNavBar
import com.github.jimmy90109.geoalarm.ui.components.NavTab
import com.github.jimmy90109.geoalarm.ui.viewmodel.HomeViewModel
import com.github.jimmy90109.geoalarm.ui.viewmodel.SettingsViewModel
import com.github.jimmy90109.geoalarm.ui.viewmodel.ViewModelFactory

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
    val isSettings = navBackStackEntry?.destination?.hasRoute<MainRoutes.Settings>() == true

    // Create the factory with dependencies
    val context = LocalContext.current
    val app = context.applicationContext as GeoAlarmApplication
    val factory = ViewModelFactory(
        app, app.repository, app.settingsRepository
    )

    // Get the SettingsViewModel
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)

    // Animation States
    val alignmentBias by animateFloatAsState(
        targetValue = if (isSettings) 0f else -1f, label = "bias"
    )
    val startPadding by animateDpAsState(
        targetValue = if (isSettings) 0.dp else 16.dp, label = "padding"
    )

    // Handle back press to go Home if on Settings
    BackHandler(enabled = isSettings) {
        navController.navigate(MainRoutes.Home) {
            popUpTo(navController.graph.id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController, startDestination = MainRoutes.Home
        ) {
            composable<MainRoutes.Home>(enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it / 2 }, animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            }, exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it / 2 }, animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }, popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it / 2 }, animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            }, popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it / 2 }, animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }) {
                HomeScreen(
                    viewModel = viewModel,
                    onAddAlarm = onAddAlarm,
                    onAlarmClick = { alarm -> onAlarmClick(alarm.id) },
                    onNavigateToBatteryOptimization = onNavigateToBatteryOptimization
                )
            }

            composable<MainRoutes.Settings>(enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it / 2 }, animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            }, exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it / 2 }, animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }, popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it / 2 }, animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            }, popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it / 2 }, animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }) {
                SettingsScreen(
                    viewModel = settingsViewModel
                )
            }
        }

        BottomNavBar(
            currentTab = if (isSettings) NavTab.SETTINGS else NavTab.HOME,
            onHomeClick = {
                // Dimiss any open settings sheets
                settingsViewModel.dismissLanguageSheet()
                settingsViewModel.dismissMonitoringSheet()

                if (isSettings) {
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
                .padding(
                    start = startPadding,
                    bottom = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding() + 8.dp
                ),
        )
    }
}
