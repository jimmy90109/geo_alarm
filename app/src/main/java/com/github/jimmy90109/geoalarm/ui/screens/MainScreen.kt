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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.jimmy90109.geoalarm.navigation.MainRoutes
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.ui.components.AppNavigationRail
import com.github.jimmy90109.geoalarm.ui.components.BottomNavBar
import com.github.jimmy90109.geoalarm.ui.components.HomeFabMenu
import com.github.jimmy90109.geoalarm.ui.components.NavTab
import com.github.jimmy90109.geoalarm.ui.viewmodel.HomeViewModel
import com.github.jimmy90109.geoalarm.ui.viewmodel.SettingsViewModel

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
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val alarms by viewModel.alarms.collectAsStateWithLifecycle(initialValue = emptyList())
    val homeUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeAlarm = homeUiState.testActiveAlarm ?: alarms.find { it.isEnabled }
    var showHomeFabMenu by remember { mutableStateOf(false) }
    // Determine if we are on a top-level tab
    val isSettings = navBackStackEntry?.destination?.hasRoute<MainRoutes.Settings>() == true
    val isPlaceReminders = navBackStackEntry?.destination?.hasRoute<MainRoutes.PlaceReminders>() == true

    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val showSettingsUpdateDot = false
    val currentTab = when {
        isSettings -> NavTab.SETTINGS
        isPlaceReminders -> NavTab.REMINDERS
        else -> NavTab.HOME
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
                showSettingsUpdateDot = showSettingsUpdateDot
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
                    onAddPlaceReminder = onAddPlaceReminder,
                    onPlaceReminderClick = onPlaceReminderClick,
                    onOpenOnboarding = onOpenOnboarding,
                    isLandscape = true
                )
                TopLevelMainFab(
                    currentTab = currentTab,
                    showHomeFabMenu = showHomeFabMenu,
                    alarms = alarms,
                    showAlarmFab = activeAlarm == null,
                    onDismissHomeFabMenu = { showHomeFabMenu = false },
                    onToggleHomeFabMenu = { showHomeFabMenu = !showHomeFabMenu },
                    onAddSchedule = {
                        showHomeFabMenu = false
                        onAddSchedule()
                    },
                    onAddAlarm = {
                        showHomeFabMenu = false
                        onAddAlarm()
                    },
                    onAddPlaceReminder = onAddPlaceReminder,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                )
            }
        }
    } else {
        // Portrait Layout: Content + Floating Bottom Bar

        // Animation States (Only for Portrait)
        val shouldCenterBottomBar = currentTab == NavTab.SETTINGS

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
                onAddPlaceReminder = onAddPlaceReminder,
                onPlaceReminderClick = onPlaceReminderClick,
                onOpenOnboarding = onOpenOnboarding,
                isLandscape = false
            )

            TopLevelMainFab(
                currentTab = currentTab,
                showHomeFabMenu = showHomeFabMenu,
                alarms = alarms,
                showAlarmFab = activeAlarm == null,
                onDismissHomeFabMenu = { showHomeFabMenu = false },
                onToggleHomeFabMenu = { showHomeFabMenu = !showHomeFabMenu },
                onAddSchedule = {
                    showHomeFabMenu = false
                    onAddSchedule()
                },
                onAddAlarm = {
                    showHomeFabMenu = false
                    onAddAlarm()
                },
                onAddPlaceReminder = onAddPlaceReminder,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 16.dp,
                        bottom = WindowInsets.navigationBars.asPaddingValues()
                            .calculateBottomPadding() + 16.dp
                    ),
            )

            BottomNavBar(
                currentTab = currentTab,
                onHomeClick = onHomeClick,
                onRemindersClick = onRemindersClick,
                onSettingsClick = onSettingsClick,
                showSettingsUpdateDot = showSettingsUpdateDot,
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TopLevelMainFab(
    currentTab: NavTab,
    showHomeFabMenu: Boolean,
    alarms: List<com.github.jimmy90109.geoalarm.data.Alarm>,
    showAlarmFab: Boolean,
    onDismissHomeFabMenu: () -> Unit,
    onToggleHomeFabMenu: () -> Unit,
    onAddSchedule: () -> Unit,
    onAddAlarm: () -> Unit,
    onAddPlaceReminder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    if (currentTab == NavTab.HOME && showHomeFabMenu) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    onDismissHomeFabMenu()
                }
                .zIndex(1f),
        )
    }

    Box(modifier = modifier.zIndex(2f)) {
        when (currentTab) {
            NavTab.HOME -> {
                if (showAlarmFab) {
                    HomeFabMenu(
                        modifier = Modifier.offset(y = 16.dp),
                        expanded = showHomeFabMenu,
                        onToggle = onToggleHomeFabMenu,
                        alarms = alarms,
                        onAddSchedule = onAddSchedule,
                        onAddAlarm = onAddAlarm,
                    )
                }
            }

            NavTab.REMINDERS -> {
                FloatingActionButtonMenu(
                    modifier = Modifier.offset(y = 16.dp),
                    expanded = false,
                    horizontalAlignment = Alignment.End,
                    button = {
                        LargeFloatingActionButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onAddPlaceReminder()
                            },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = stringResource(R.string.place_reminder_add),
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    },
                ) {
                }
            }

            NavTab.SETTINGS -> Unit
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
                onOpenOnboarding = onOpenOnboarding
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
                viewModel = hiltViewModel(),
                onAddReminder = onAddPlaceReminder,
                onReminderClick = onPlaceReminderClick,
            )
        }
    }
}
