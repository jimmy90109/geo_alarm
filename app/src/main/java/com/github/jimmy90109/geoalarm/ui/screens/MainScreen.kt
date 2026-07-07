package com.github.jimmy90109.geoalarm.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.jimmy90109.geoalarm.navigation.MainRoutes
import com.github.jimmy90109.geoalarm.ui.components.AppNavigationRail
import com.github.jimmy90109.geoalarm.ui.components.BottomNavBar
import com.github.jimmy90109.geoalarm.ui.components.NavTab
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.PlaceReminderListScreen
import com.github.jimmy90109.geoalarm.ui.viewmodel.HomeViewModel
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderListViewModel
import com.github.jimmy90109.geoalarm.ui.viewmodel.SettingsViewModel
import com.github.jimmy90109.geoalarm.utils.SharedPreferenceManager

@Composable
fun MainScreen(
    viewModel: HomeViewModel,
    onAddAlarm: () -> Unit,
    onAlarmClick: (String) -> Unit,
    onAddSchedule: () -> Unit,
    onScheduleClick: (String) -> Unit,
    onAddPlaceReminder: () -> Unit,
    onPlaceReminderClick: (String) -> Unit,
    onOpenOnboarding: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    // Determine if we are on a top-level tab
    val isSettings = navBackStackEntry?.destination?.hasRoute<MainRoutes.Settings>() == true
    val isPlaceReminders = navBackStackEntry?.destination?.hasRoute<MainRoutes.PlaceReminders>() == true

    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val placeReminderListViewModel: PlaceReminderListViewModel = hiltViewModel()
    placeReminderListViewModel.listState.collectAsStateWithLifecycle()
    val sharedPreferenceManager = remember(context) {
        SharedPreferenceManager(context.applicationContext)
    }
    var hasSeenPlaceReminderTab by remember {
        mutableStateOf(sharedPreferenceManager.hasSeenPlaceReminderTab)
    }
    val currentTab = when {
        isSettings -> NavTab.SETTINGS
        isPlaceReminders -> NavTab.REMINDERS
        else -> NavTab.HOME
    }
    val showRemindersBadge = !hasSeenPlaceReminderTab && !isPlaceReminders

    fun markPlaceReminderTabSeen() {
        if (hasSeenPlaceReminderTab) return
        hasSeenPlaceReminderTab = true
        sharedPreferenceManager.hasSeenPlaceReminderTab = true
    }

    LaunchedEffect(isPlaceReminders) {
        if (isPlaceReminders) {
            markPlaceReminderTabSeen()
        }
    }

    // Navigation Actions
    val onHomeClick: () -> Unit = {
        if (currentTab != NavTab.HOME) {
            navController.navigate(MainRoutes.Home) {
                popUpTo(navController.graph.id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val onRemindersClick: () -> Unit = {
        markPlaceReminderTabSeen()
        if (currentTab != NavTab.REMINDERS) {
            navController.navigate(MainRoutes.PlaceReminders) {
                popUpTo(navController.graph.id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val onSettingsClick: () -> Unit = {
        if (currentTab != NavTab.SETTINGS) {
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
    BackHandler(enabled = currentTab != NavTab.HOME) {
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
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppNavigationRail(
                currentTab = currentTab,
                onHomeClick = onHomeClick,
                onRemindersClick = onRemindersClick,
                onSettingsClick = onSettingsClick,
                showRemindersBadge = showRemindersBadge,
            )

            Box(modifier = Modifier.weight(1f)) {
                MainNavHost(
                    navController = navController,
                    viewModel = viewModel,
                    settingsViewModel = settingsViewModel,
                    placeReminderListViewModel = placeReminderListViewModel,
                    onAddAlarm = onAddAlarm,
                    onAlarmClick = onAlarmClick,
                    onAddSchedule = onAddSchedule,
                    onScheduleClick = onScheduleClick,
                    onAddPlaceReminder = onAddPlaceReminder,
                    onPlaceReminderClick = onPlaceReminderClick,
                    onOpenOnboarding = onOpenOnboarding,
                    isLandscape = true
                )
            }
        }
    } else {
        // Portrait Layout: Content + Floating Bottom Bar

        Box(modifier = Modifier.fillMaxSize()) {
            MainNavHost(
                navController = navController,
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
                placeReminderListViewModel = placeReminderListViewModel,
                onAddAlarm = onAddAlarm,
                onAlarmClick = onAlarmClick,
                onAddSchedule = onAddSchedule,
                onScheduleClick = onScheduleClick,
                onAddPlaceReminder = onAddPlaceReminder,
                onPlaceReminderClick = onPlaceReminderClick,
                onOpenOnboarding = onOpenOnboarding,
                isLandscape = false
            )

            BottomNavBar(
                currentTab = currentTab,
                onHomeClick = onHomeClick,
                onRemindersClick = onRemindersClick,
                onSettingsClick = onSettingsClick,
                showRemindersBadge = showRemindersBadge,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
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
    placeReminderListViewModel: PlaceReminderListViewModel,
    onAddAlarm: () -> Unit,
    onAlarmClick: (String) -> Unit,
    onAddSchedule: () -> Unit,
    onScheduleClick: (String) -> Unit,
    onAddPlaceReminder: () -> Unit,
    onPlaceReminderClick: (String) -> Unit,
    onOpenOnboarding: () -> Unit,
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
                onOpenOnboarding = onOpenOnboarding,
                isLandscape = isLandscape,
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

        composable<MainRoutes.PlaceReminders>(
            enterTransition = {
                if (isLandscape) {
                    slideInVertically(
                        initialOffsetY = { it / 3 }, animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                } else {
                    slideInHorizontally(
                        initialOffsetX = { it / 3 }, animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                }
            },
            exitTransition = {
                if (isLandscape) {
                    slideOutVertically(
                        targetOffsetY = { it / 3 }, animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                } else {
                    slideOutHorizontally(
                        targetOffsetX = { it / 3 }, animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                }
            },
        ) {
            PlaceReminderListScreen(
                viewModel = placeReminderListViewModel,
                onAddReminder = onAddPlaceReminder,
                onReminderClick = onPlaceReminderClick,
                isLandscape = isLandscape,
            )
        }
    }
}
