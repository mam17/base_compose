package com.example.basecompose.presentation.language

import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.basecompose.base.findActivity
import com.example.basecompose.navigation.navigateAndClearBackStack
import com.example.basecompose.presentation.onboarding.OnboardingRoute
import kotlinx.serialization.Serializable

@Serializable
data object LanguageRoute

fun NavGraphBuilder.languageScreen(navController: NavController) {
    composable<LanguageRoute> {
        val activity = LocalContext.current.findActivity()
        LanguageScreen(
            onLanguageApplied = { needRecreate ->
                // Điều hướng TRƯỚC rồi mới recreate: back stack (đang ở Onboarding) được
                // NavController lưu và khôi phục, nên sau recreate app hiện thẳng Onboarding
                // với ngôn ngữ mới.
                navController.navigateAndClearBackStack(OnboardingRoute)
                if (needRecreate) activity?.recreate()
            },
        )
    }
}
