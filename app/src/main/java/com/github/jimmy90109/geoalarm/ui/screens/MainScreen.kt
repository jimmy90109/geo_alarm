package com.github.jimmy90109.geoalarm.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.jimmy90109.geoalarm.GeoAlarmApplication
import com.github.jimmy90109.geoalarm.navigation.MainRoutes
import com.github.jimmy90109.geoalarm.ui.components.AppNavigationRail
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
    onAddSchedule: () -> Unit,
    onScheduleClick: (String) -> Unit,
    onNavigateToBatteryOptimization: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val alarms by viewModel.alarms.collectAsStateWithLifecycle(initialValue = emptyList())
    val hasActiveAlarm = alarms.any { it.isEnabled }

    // Determine if we are on a top-level tab
    val isSettings = navBackStackEntry?.destination?.hasRoute<MainRoutes.Settings>() == true

    // Create the factory with dependencies
    val context = LocalContext.current
    val app = context.applicationContext as GeoAlarmApplication
    val factory = ViewModelFactory(
        app,
        app.repository,
        app.settingsRepository,
        app.sharedPreferenceManager,
    )

    // Get the SettingsViewModel
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)

    // Navigation Actions
    val onHomeClick: () -> Unit = {
        if (isSettings) {
            navController.navigate(MainRoutes.Home) {
                popUpTo(navController.graph.id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val onSettingsClick: () -> Unit = {
        if (!isSettings) {
            navController.navigate(MainRoutes.Settings) {
                popUpTo(navController.graph.id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

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

    if (isLandscape) {
        // Landscape Layout: Navigation Rail + Content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.displayCutout),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppNavigationRail(
                currentTab = if (isSettings) NavTab.SETTINGS else NavTab.HOME,
                onHomeClick = onHomeClick,
                onSettingsClick = onSettingsClick
            )

            Box(modifier = Modifier.weight(1f)) {
                MainNavHost(
                    navController = navController,
                    viewModel = viewModel,
                    settingsViewModel = settingsViewModel,
                    onAddAlarm = onAddAlarm,
                    onAlarmClick = onAlarmClick,
                    onAddSchedule = onAddSchedule,
                    onScheduleClick = onScheduleClick,
                    onNavigateToBatteryOptimization = onNavigateToBatteryOptimization,
                    isLandscape = true
                )
            }
        }
    } else {
        // Portrait Layout: Content + Floating Bottom Bar

        // Animation States (Only for Portrait)
        // Center BottomNavBar if on Settings OR if an alarm is active (ActiveAlarmScreen)
        val shouldCenterBottomBar = isSettings || hasActiveAlarm

        val alignmentBias by animateFloatAsState(
            targetValue = if (shouldCenterBottomBar) 0f else -1f, label = "bias"
        )
        val startPadding by animateDpAsState(
            targetValue = if (shouldCenterBottomBar) 0.dp else 16.dp, label = "padding"
        )

        Box(modifier = Modifier.fillMaxSize()) {
            MainNavHost(
                navController = navController,
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
                onAddAlarm = onAddAlarm,
                onAlarmClick = onAlarmClick,
                onAddSchedule = onAddSchedule,
                onScheduleClick = onScheduleClick,
                onNavigateToBatteryOptimization = onNavigateToBatteryOptimization,
                isLandscape = false
            )

            BottomNavBar(
                currentTab = if (isSettings) NavTab.SETTINGS else NavTab.HOME,
                onHomeClick = onHomeClick,
                onSettingsClick = onSettingsClick,
                modifier = Modifier
                    .align(BiasAlignment(alignmentBias, 1f))
                    .padding(
                        start = startPadding,
                        bottom = WindowInsets.navigationBars.asPaddingValues()
                            .calculateBottomPadding() + 16.dp
                    ),
            )
        }
    }
}

@Composable
fun MainNavHost(
    navController: androidx.navigation.NavHostController,
    viewModel: HomeViewModel,
    settingsViewModel: SettingsViewModel,
    onAddAlarm: () -> Unit,
    onAlarmClick: (String) -> Unit,
    onAddSchedule: () -> Unit,
    onScheduleClick: (String) -> Unit,
    onNavigateToBatteryOptimization: () -> Unit,
    isLandscape: Boolean
) {
    NavHost(
        navController = navController, startDestination = MainRoutes.Home
    ) {
        composable<MainRoutes.Home>(
            enterTransition = {
                if (isLandscape) {
                    slideInVertically(
                        initialOffsetY = { -it / 2 }, animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                } else {
                    slideInHorizontally(
                        initialOffsetX = { -it / 2 }, animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                }
            },
            exitTransition = {
                if (isLandscape) {
                    slideOutVertically(
                        targetOffsetY = { -it / 2 }, animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                } else {
                    slideOutHorizontally(
                        targetOffsetX = { -it / 2 }, animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                }
            },
            popEnterTransition = {
                if (isLandscape) {
                    slideInVertically(
                        initialOffsetY = { -it / 2 }, animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                } else {
                    slideInHorizontally(
                        initialOffsetX = { -it / 2 }, animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                }
            },
            popExitTransition = {
                if (isLandscape) {
                    slideOutVertically(
                        targetOffsetY = { -it / 2 }, animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                } else {
                    slideOutHorizontally(
                        targetOffsetX = { -it / 2 }, animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                }
            },
        ) {
            HomeScreen(
                viewModel = viewModel,
                onAddAlarm = onAddAlarm,
                onAlarmClick = { alarm -> onAlarmClick(alarm.id) },
                onAddSchedule = onAddSchedule,
                onScheduleClick = { schedule -> onScheduleClick(schedule.schedule.id) },
                onNavigateToBatteryOptimization = onNavigateToBatteryOptimization
            )
        }

        composable<MainRoutes.Settings>(
            enterTransition = {
                if (isLandscape) {
                    slideInVertically(
                        initialOffsetY = { it / 2 }, animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                } else {
                    slideInHorizontally(
                        initialOffsetX = { it / 2 }, animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                }
            },
            exitTransition = {
                if (isLandscape) {
                    slideOutVertically(
                        targetOffsetY = { it / 2 }, animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                } else {
                    slideOutHorizontally(
                        targetOffsetX = { it / 2 }, animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                }
            },
            popEnterTransition = {
                if (isLandscape) {
                    slideInVertically(
                        initialOffsetY = { it / 2 }, animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                } else {
                    slideInHorizontally(
                        initialOffsetX = { it / 2 }, animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                }
            },
            popExitTransition = {
                if (isLandscape) {
                    slideOutVertically(
                        targetOffsetY = { it / 2 }, animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                } else {
                    slideOutHorizontally(
                        targetOffsetX = { it / 2 }, animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                }
            },
        ) {
            SettingsScreen(
                viewModel = settingsViewModel
            )
        }
    }
}
