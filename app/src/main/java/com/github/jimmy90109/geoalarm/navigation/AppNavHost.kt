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
import com.github.jimmy90109.geoalarm.ui.viewmodel.AlarmEditViewModel
import com.github.jimmy90109.geoalarm.ui.viewmodel.HomeViewModel
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
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController, startDestination = AppRoutes.Main, modifier = modifier,
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
        composable<AppRoutes.Main> {
            val viewModel: HomeViewModel = viewModel(factory = viewModelFactory)
            AnimatedNavScreen {
                MainScreen(
                    viewModel = viewModel,
                    onAddAlarm = { navController.navigate(AppRoutes.AlarmEdit()) },
                    onAlarmClick = { alarmId ->
                        navController.navigate(AppRoutes.AlarmEdit(alarmId))
                    },
                    onNavigateToBatteryOptimization = {
                        navController.navigate(AppRoutes.BatteryOptimization)
                    },
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
                    onNavigateBack = { navController.popBackStack() })
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
    }
}
