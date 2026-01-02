package com.example.geo_alarm.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.geo_alarm.ui.screens.AlarmEditScreen
import com.example.geo_alarm.ui.screens.HomeScreen
import com.example.geo_alarm.ui.screens.BatteryOptimizationScreen
import com.example.geo_alarm.ui.viewmodel.AlarmEditViewModel
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import com.example.geo_alarm.ui.viewmodel.HomeViewModel
import com.example.geo_alarm.ui.viewmodel.ViewModelFactory

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
        startDestination = AppRoutes.Home,
        modifier = modifier,
        // Predictive back animation
        popEnterTransition = { EnterTransition.None },
        popExitTransition = {
            scaleOut(
                targetScale = 0.9f,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable<AppRoutes.Home> {
            val viewModel: HomeViewModel = viewModel(factory = viewModelFactory)
            HomeScreen(
                viewModel = viewModel,
                onAddAlarm = { navController.navigate(AppRoutes.AlarmEdit()) },
                onAlarmClick = { alarm ->
                    navController.navigate(AppRoutes.AlarmEdit(alarm.id))
                },
                onNavigateToBatteryOptimization = {
                    navController.navigate(AppRoutes.BatteryOptimization)
                }
            )
        }
        
        composable<AppRoutes.AlarmEdit> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoutes.AlarmEdit>()
            val viewModel: AlarmEditViewModel = viewModel(factory = viewModelFactory)
            AlarmEditScreen(
                viewModel = viewModel,
                alarmId = route.alarmId,
                onNavigateBack = { navController.popBackStack() }
            )
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
                }
            )
        }
    }
}

