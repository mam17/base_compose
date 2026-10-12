package com.example.basecompose.presentation.splash

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.basecompose.navigation.MainRoute
import com.example.basecompose.navigation.navigateAndClearBackStack
import com.example.basecompose.presentation.language.LanguageRoute
import com.example.basecompose.presentation.onboarding.OnboardingRoute
import kotlinx.serialization.Serializable

@Serializable
data object SplashRoute

/** Splash luôn bị xoá khỏi back stack sau khi điều hướng -> bấm Back không quay lại Splash. */
fun NavGraphBuilder.splashScreen(navController: NavController) {
    composable<SplashRoute> {
        SplashScreen(
            onNavigateToLanguage = { navController.navigateAndClearBackStack(LanguageRoute) },
            onNavigateToOnboarding = { navController.navigateAndClearBackStack(OnboardingRoute) },
            onNavigateToMain = { navController.navigateAndClearBackStack(MainRoute) },
        )
    }
}
