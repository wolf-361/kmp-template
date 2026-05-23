package com.yourcompany.kmptemplate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.yourcompany.kmptemplate.auth.LoginRoute
import com.yourcompany.kmptemplate.core.data.network.GlobalUiEffect
import com.yourcompany.kmptemplate.core.data.network.GlobalUiEffectsHandler
import com.yourcompany.kmptemplate.core.navigation.AuthDestination
import com.yourcompany.kmptemplate.core.navigation.Destination
import com.yourcompany.kmptemplate.core.navigation.TopLevelGraph
import com.yourcompany.kmptemplate.settings.SettingsRoute
// <create-feature:import> — imports added by create-feature.sh go here

@Composable
fun AppNavHost(navController: NavHostController, globalEffectsHandler: GlobalUiEffectsHandler) {
    LaunchedEffect(Unit) {
        globalEffectsHandler.effects.collect { effect ->
            when (effect) {
                is GlobalUiEffect.NavigateTo -> navController.navigate(effect.destination)
                is GlobalUiEffect.NavigateBack -> navController.navigateUp()
                is GlobalUiEffect.NavigateBackTo -> navController.navigate(effect.destination) {
                    popUpTo(navController.graph.id) { inclusive = effect.inclusive }
                }
                is GlobalUiEffect.Unauthorized -> navController.navigate(AuthDestination.Login) {
                    popUpTo(0) { inclusive = true }
                }
                else -> {}
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AuthDestination.Login,
    ) {
        composable<AuthDestination.Login> { LoginRoute() }
        composable<AuthDestination.Register> { PlaceholderScreen("Register") }
        composable<TopLevelGraph.Dashboard> { PlaceholderScreen("Dashboard") }
        composable<TopLevelGraph.Settings> { SettingsRoute() }
        // <create-feature:composable> — composable entries added by create-feature.sh go here
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(name)
    }
}

// Type-safe dispatch: sealed `Destination` → concrete NavController.navigate<T>()
private fun NavController.navigate(destination: Destination) {
    when (destination) {
        is AuthDestination.Login -> navigate(destination)
        is AuthDestination.Register -> navigate(destination)
        is TopLevelGraph.Dashboard -> navigate(destination)
        is TopLevelGraph.Settings -> navigate(destination)
        // <create-feature:navigate> — dispatch cases added by create-feature.sh go here
    }
}
