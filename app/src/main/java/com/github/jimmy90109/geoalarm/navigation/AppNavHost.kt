package com.github.jimmy90109.geoalarm.navigation

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.github.jimmy90109.geoalarm.ui.screens.AlarmEditScreen
import com.github.jimmy90109.geoalarm.ui.screens.BatteryOptimizationScreen
import com.github.jimmy90109.geoalarm.ui.screens.MainScreen
import com.github.jimmy90109.geoalarm.ui.screens.OnboardingScreen
import com.github.jimmy90109.geoalarm.ui.screens.ScheduleEditScreen
import com.github.jimmy90109.geoalarm.ui.viewmodel.AlarmEditViewModel
import com.github.jimmy90109.geoalarm.ui.viewmodel.HomeViewModel
import com.github.jimmy90109.geoalarm.ui.viewmodel.OnboardingViewModel
import com.github.jimmy90109.geoalarm.ui.viewmodel.ScheduleEditViewModel
import com.github.jimmy90109.geoalarm.ui.viewmodel.ViewModelFactory

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
    navController: NavHostController,
    viewModelFactory: ViewModelFactory,
    startDestination: AppRoutes = AppRoutes.Main,
    modifier: Modifier = Modifier
) {
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

            val viewModel: HomeViewModel = viewModel(
                viewModelStoreOwner = activity,
                factory = viewModelFactory
            )
            // Observe savedStateHandle for highlight requests
            val savedStateHandle = backStackEntry.savedStateHandle
            val highlightedAlarmId = savedStateHandle.get<String>("highlight_alarm_id")
            val highlightedScheduleId = savedStateHandle.get<String>("highlight_schedule_id")

            LaunchedEffect(highlightedAlarmId) {
                if (highlightedAlarmId != null) {
                    viewModel.setHighlightedAlarm(highlightedAlarmId)
                    savedStateHandle.remove<String>("highlight_alarm_id")
                }
            }
            LaunchedEffect(highlightedScheduleId) {
                if (highlightedScheduleId != null) {
                    viewModel.setHighlightedSchedule(highlightedScheduleId)
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
                    onNavigateToBatteryOptimization = {
                        navController.navigate(AppRoutes.BatteryOptimization)
                    },
                    onOpenOnboarding = { navController.navigate(AppRoutes.Onboarding) }
                )
            }
        }

        composable<AppRoutes.Onboarding> {
            val onboardingViewModel: OnboardingViewModel = viewModel(factory = viewModelFactory)
            AnimatedNavScreen {
                OnboardingScreen(
                    viewModel = onboardingViewModel,
                    onFinished = {
                        if (navController.previousBackStackEntry != null) {
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
            val viewModel: AlarmEditViewModel = viewModel(factory = viewModelFactory)
            AnimatedNavScreen {
                AlarmEditScreen(
                    viewModel = viewModel,
                    alarmId = route.alarmId,
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

        composable<AppRoutes.BatteryOptimization> {
            val context = LocalContext.current
            AnimatedNavScreen {
                BatteryOptimizationScreen(
                    onFix = {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    },
                    onOptimizationDisabled = {
                        navController.popBackStack()
                    },
                )
            }
        }

        composable<AppRoutes.ScheduleEdit> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoutes.ScheduleEdit>()
            val viewModel: ScheduleEditViewModel = viewModel(factory = viewModelFactory)
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
    }
}
