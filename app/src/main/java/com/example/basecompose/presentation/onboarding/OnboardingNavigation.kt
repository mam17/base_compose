package com.example.basecompose.presentation.onboarding

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.basecompose.navigation.MainRoute
import com.example.basecompose.navigation.navigateAndClearBackStack
import kotlinx.serialization.Serializable

@Serializable
data object OnboardingRoute

fun NavGraphBuilder.onboardingScreen(navController: NavController) {
    composable<OnboardingRoute> {
        OnboardingScreen(
            onNavigateToMain = { navController.navigateAndClearBackStack(MainRoute) },
        )
    }
}
