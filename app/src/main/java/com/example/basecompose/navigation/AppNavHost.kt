package com.example.basecompose.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.basecompose.GameScreen
import com.example.basecompose.presentation.example.exampleGraph
import com.example.basecompose.presentation.language.languageScreen
import com.example.basecompose.presentation.onboarding.onboardingScreen
import com.example.basecompose.presentation.splash.SplashRoute
import com.example.basecompose.presentation.splash.splashScreen
import kotlinx.serialization.Serializable

/** Màn chính của app (hiện là màn game). Luồng mở app: Splash -> Language -> Onboarding -> Main. */
@Serializable
data object MainRoute

private const val TRANSITION_MILLIS = 300

/**
 * NavHost gốc của app, đặt trong [com.example.basecompose.base.BaseApp].
 *
 * Quy ước:
 * - Route là `@Serializable` object/data class (type-safe, không dùng string).
 * - Mỗi feature khai báo graph của mình trong `XxxNavigation.kt` (vd: [exampleGraph]),
 *   ở đây chỉ gom các graph lại -> thêm feature không phải sửa file khác ngoài dòng gọi graph.
 * - Điều hướng qua [navigateFrom] / [popBackStackFrom] để chống bấm liên tục.
 *
 * @param startDestination màn đầu tiên; luôn là Splash, Splash sẽ quyết định màn tiếp theo
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: Any = SplashRoute,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { slideIntoContainer(SlideDirection.Start, tween(TRANSITION_MILLIS)) },
        exitTransition = { fadeOut(tween(TRANSITION_MILLIS)) },
        popEnterTransition = { fadeIn(tween(TRANSITION_MILLIS)) },
        popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(TRANSITION_MILLIS)) },
    ) {
        // Luồng mở app
        splashScreen(navController)
        languageScreen(navController)
        onboardingScreen(navController)

        composable<MainRoute> {
            GameScreen()
        }

        exampleGraph(navController)
    }
}
