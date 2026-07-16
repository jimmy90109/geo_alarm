package com.github.jimmy90109.geoalarm.navigation

import android.view.RoundedCorner
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.github.jimmy90109.geoalarm.data.DEFAULT_ALARM_ICON_KEY
import com.github.jimmy90109.geoalarm.ui.screens.AlarmEditScreen
import com.github.jimmy90109.geoalarm.ui.screens.MainScreen
import com.github.jimmy90109.geoalarm.ui.screens.OnboardingScreen
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.PlaceReminderDetailScreen
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.PlaceReminderEditScreen
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.PlaceReminderPlacePickerScreen
import com.github.jimmy90109.geoalarm.ui.screens.ScheduleEditScreen
import com.github.jimmy90109.geoalarm.ui.viewmodel.AlarmEditViewModel
import com.github.jimmy90109.geoalarm.ui.viewmodel.HomeAction
import com.github.jimmy90109.geoalarm.ui.viewmodel.HomeViewModel
import com.github.jimmy90109.geoalarm.ui.viewmodel.OnboardingViewModel
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderDetailViewModel
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderEditAction
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderEditViewModel
import com.github.jimmy90109.geoalarm.ui.viewmodel.ScheduleEditViewModel

// Material 3 Motion constants
private const val DURATION_MEDIUM = 300
private const val DURATION_SHORT = 150
private const val INITIAL_SCALE = 0.92f

/**
 * Wrapper composable that applies device's corner radius during navigation transitions.
 */
@Composable
private fun AnimatedNavScreen(
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val view = LocalView.current

    val cornerRadius = remember(view) {
        val windowInsets = view.rootWindowInsets

        val corner = windowInsets?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)

        if (corner != null) {
            with(density) { corner.radius.toDp() }
        } else {
            24.dp // fallback
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()
    }
}

/**
 * Main navigation host for the app.
 * Separates navigation logic from MainActivity for better testability.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: AppRoutes = AppRoutes.Main,
    requestedDestination: AppRoutes? = null,
) {
    var pendingRequestedDestination by remember(requestedDestination) {
        mutableStateOf(requestedDestination)
    }

    LaunchedEffect(startDestination, pendingRequestedDestination) {
        val pendingDestination = pendingRequestedDestination
        if (startDestination == AppRoutes.Main && pendingDestination != null) {
            pendingRequestedDestination = null
            navController.navigate(pendingDestination) {
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController, startDestination = startDestination, modifier = modifier,
        // Global default animations for all routes
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it / 5 },
                animationSpec = tween(DURATION_MEDIUM, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(DURATION_MEDIUM)) + scaleIn(
                initialScale = INITIAL_SCALE,
                animationSpec = tween(DURATION_MEDIUM, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(DURATION_SHORT))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 5 },
                animationSpec = tween(DURATION_MEDIUM, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(DURATION_MEDIUM))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it / 5 },
                animationSpec = tween(DURATION_MEDIUM, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(DURATION_SHORT)) + scaleOut(
                targetScale = INITIAL_SCALE,
                animationSpec = tween(DURATION_MEDIUM, easing = FastOutSlowInEasing)
            )
        },
    ) {
        composable<AppRoutes.Main> { backStackEntry ->
            val context = LocalContext.current
            // Use Activity scope for HomeViewModel to share state with MainActivity intent handling
            val activity = remember(context) { 
                var ctx = context
                while (ctx is android.content.ContextWrapper) {
                    if (ctx is androidx.activity.ComponentActivity) return@remember ctx
                    ctx = ctx.baseContext
                }
                null
            } ?: throw IllegalStateException("Context is not a ComponentActivity")

            val viewModel: HomeViewModel = hiltViewModel(activity)
            // Observe savedStateHandle for highlight requests
            val savedStateHandle = backStackEntry.savedStateHandle
            val highlightedAlarmId = savedStateHandle.get<String>("highlight_alarm_id")
            val highlightedScheduleId = savedStateHandle.get<String>("highlight_schedule_id")

            LaunchedEffect(highlightedAlarmId) {
                if (highlightedAlarmId != null) {
                    viewModel.onAction(HomeAction.AlarmHighlighted(highlightedAlarmId))
                    savedStateHandle.remove<String>("highlight_alarm_id")
                }
            }
            LaunchedEffect(highlightedScheduleId) {
                if (highlightedScheduleId != null) {
                    viewModel.onAction(HomeAction.ScheduleHighlighted(highlightedScheduleId))
                    savedStateHandle.remove<String>("highlight_schedule_id")
                }
            }

            AnimatedNavScreen {
                MainScreen(
                    viewModel = viewModel,
                    onAddAlarm = { navController.navigate(AppRoutes.AlarmEdit()) },
                    onAlarmClick = { alarmId ->
                        navController.navigate(AppRoutes.AlarmEdit(alarmId))
                    },
                    onAddSchedule = { navController.navigate(AppRoutes.ScheduleEdit()) },
                    onScheduleClick = { scheduleId ->
                        navController.navigate(AppRoutes.ScheduleEdit(scheduleId))
                    },
                    onAddPlaceReminder = {
                        navController.navigate(
                            AppRoutes.PlaceReminderPlacePicker(createEditOnComplete = true)
                        )
                    },
                    onPlaceReminderClick = { reminderId ->
                        navController.navigate(AppRoutes.PlaceReminderDetail(reminderId))
                    },
                    onOpenOnboarding = {
                        navController.navigate(AppRoutes.Onboarding)
                    }
                )
            }
        }

        composable<AppRoutes.Onboarding> {
            val onboardingViewModel: OnboardingViewModel = hiltViewModel()
            AnimatedNavScreen {
                OnboardingScreen(
                    viewModel = onboardingViewModel,
                    onFinished = {
                        val pendingDestination = pendingRequestedDestination
                        if (pendingDestination != null) {
                            pendingRequestedDestination = null
                            navController.navigate(AppRoutes.Main) {
                                popUpTo(AppRoutes.Onboarding) { inclusive = true }
                                launchSingleTop = true
                            }
                            navController.navigate(pendingDestination) {
                                launchSingleTop = true
                            }
                        } else if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(AppRoutes.Main) {
                                popUpTo(AppRoutes.Onboarding) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }

        composable<AppRoutes.AlarmEdit> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoutes.AlarmEdit>()
            val viewModel: AlarmEditViewModel = hiltViewModel()
            AnimatedNavScreen {
                AlarmEditScreen(
                    viewModel = viewModel,
                    alarmId = route.alarmId,
                    sharedPlaceQuery = route.sharedPlaceQuery,
                    sharedPlaceSource = route.sharedPlaceSource,
                    onNavigateBack = {
                        val state = viewModel.uiState.value
                        if (state.isSaved && state.savedAlarmId != null) {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("highlight_alarm_id", state.savedAlarmId)
                        }
                        navController.popBackStack()
                    })
            }
        }

        composable<AppRoutes.ScheduleEdit> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoutes.ScheduleEdit>()
            val viewModel: ScheduleEditViewModel = hiltViewModel()
            AnimatedNavScreen {
                ScheduleEditScreen(
                    viewModel = viewModel,
                    scheduleId = route.scheduleId,
                    onBack = {
                        val state = viewModel.uiState.value
                        // We check savedScheduleId from ViewModel which we added previously
                        if (state.savedScheduleId != null) {
                             navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("highlight_schedule_id", state.savedScheduleId)
                        }
                        navController.popBackStack()
                    }
                )
            }
        }

        composable<AppRoutes.PlaceReminderEdit> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoutes.PlaceReminderEdit>()
            val viewModel: PlaceReminderEditViewModel = hiltViewModel()
            LaunchedEffect(route) {
                val latitude = route.initialLatitude
                val longitude = route.initialLongitude
                val placeName = route.initialPlaceName
                if (latitude != null && longitude != null && !placeName.isNullOrBlank()) {
                    viewModel.onAction(
                        PlaceReminderEditAction.ApplyInitialPlace(
                            latitude = latitude,
                            longitude = longitude,
                            placeName = placeName,
                            address = route.initialAddress,
                            iconKey = route.initialIconKey ?: DEFAULT_ALARM_ICON_KEY,
                            radiusMeters = route.initialRadiusMeters ?: 1000,
                        )
                    )
                }
            }

            AnimatedNavScreen {
                PlaceReminderEditScreen(
                    viewModel = viewModel,
                    reminderId = route.reminderId,
                    onSelectPlace = {
                        navController.navigate(AppRoutes.PlaceReminderPlacePicker(route.reminderId))
                    },
                    onBack = { savedReminderId ->
                        if (savedReminderId != null) {
                            navController.navigate(AppRoutes.PlaceReminderDetail(savedReminderId)) {
                                popUpTo(route) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                )
            }
        }

        composable<AppRoutes.PlaceReminderPlacePicker> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoutes.PlaceReminderPlacePicker>()
            val viewModel: PlaceReminderEditViewModel = if (route.createEditOnComplete) {
                hiltViewModel()
            } else {
                val editBackStackEntry = remember(navController, backStackEntry, route.reminderId) {
                    navController.previousBackStackEntry
                        ?: navController.getBackStackEntry(AppRoutes.PlaceReminderEdit(route.reminderId))
                }
                hiltViewModel(editBackStackEntry)
            }
            LaunchedEffect(route.createEditOnComplete) {
                if (route.createEditOnComplete) {
                    viewModel.onAction(PlaceReminderEditAction.Load(null))
                }
            }
            AnimatedNavScreen {
                PlaceReminderPlacePickerScreen(
                    viewModel = viewModel,
                    reminderId = route.reminderId,
                    onPlaceSelected = {
                        if (route.createEditOnComplete) {
                            val state = viewModel.uiState.value
                            val position = state.selectedPosition
                            if (position != null && state.placeName.isNotBlank()) {
                                navController.navigate(
                                    AppRoutes.PlaceReminderEdit(
                                        initialLatitude = position.latitude,
                                        initialLongitude = position.longitude,
                                        initialPlaceName = state.placeName,
                                        initialAddress = state.address,
                                        initialIconKey = state.selectedIconKey,
                                        initialRadiusMeters = state.radiusMeters,
                                    )
                                ) {
                                    popUpTo(route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                    onCancel = {
                        navController.popBackStack()
                    },
                )
            }
        }

        composable<AppRoutes.PlaceReminderDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoutes.PlaceReminderDetail>()
            val viewModel: PlaceReminderDetailViewModel = hiltViewModel()
            AnimatedNavScreen {
                PlaceReminderDetailScreen(
                    viewModel = viewModel,
                    reminderId = route.reminderId,
                    onBack = { navController.popBackStack() },
                    onEdit = { reminderId ->
                        navController.navigate(AppRoutes.PlaceReminderEdit(reminderId))
                    },
                )
            }
        }
    }
}
