package com.example.geo_alarm.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.geo_alarm.data.AlarmRepository
import com.example.geo_alarm.ui.screens.AlarmEditScreen
import com.example.geo_alarm.ui.screens.HomeScreen

/**
 * Main navigation host for the app.
 * Separates navigation logic from MainActivity for better testability.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    repository: AlarmRepository,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.Home,
        modifier = modifier
    ) {
        composable<AppRoutes.Home> {
            HomeScreen(
                repository = repository,
                onAddAlarm = { navController.navigate(AppRoutes.AlarmEdit()) },
                onAlarmClick = { alarm ->
                    navController.navigate(AppRoutes.AlarmEdit(alarm.id))
                }
            )
        }
        
        composable<AppRoutes.AlarmEdit> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoutes.AlarmEdit>()
            AlarmEditScreen(
                repository = repository,
                alarmId = route.alarmId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
