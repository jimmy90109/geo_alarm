package com.github.jimmy90109.geoalarm.navigation

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
        navController = navController,
        startDestination = AppRoutes.Main,
        modifier = modifier,
    ) {
        composable<AppRoutes.Main>(
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
        ) {
            val viewModel: HomeViewModel = viewModel(factory = viewModelFactory)
            MainScreen(
                viewModel = viewModel,
                onAddAlarm = { navController.navigate(AppRoutes.AlarmEdit()) },
                onAlarmClick = { alarmId ->
                    navController.navigate(AppRoutes.AlarmEdit(alarmId))
                },
                onNavigateToBatteryOptimization = {
                    navController.navigate(AppRoutes.BatteryOptimization)
                })
        }

        composable<AppRoutes.AlarmEdit> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoutes.AlarmEdit>()
            val viewModel: AlarmEditViewModel = viewModel(factory = viewModelFactory)
            AlarmEditScreen(
                viewModel = viewModel,
                alarmId = route.alarmId,
                onNavigateBack = { navController.popBackStack() })
        }

        composable<AppRoutes.BatteryOptimization> {
            val context = LocalContext.current
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

