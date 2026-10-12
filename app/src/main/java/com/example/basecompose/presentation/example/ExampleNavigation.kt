package com.example.basecompose.presentation.example

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.basecompose.navigation.navigateFrom
import com.example.basecompose.navigation.popBackStackFrom
import com.example.basecompose.presentation.example.detail.ExampleDetailRoute
import com.example.basecompose.presentation.example.detail.ExampleDetailScreen
import kotlinx.serialization.Serializable

/** Graph của feature Example (gồm nhiều màn). Màn khác mở feature này bằng `navigate(ExampleGraph)`. */
@Serializable
data object ExampleGraph

/** Màn danh sách. */
@Serializable
data object ExampleRoute

/**
 * Khai báo các màn của feature Example. Mỗi feature có 1 file `XxxNavigation.kt` như thế này
 * và được gọi trong [com.example.basecompose.navigation.AppNavHost].
 */
fun NavGraphBuilder.exampleGraph(navController: NavController) {
    navigation<ExampleGraph>(startDestination = ExampleRoute) {
        composable<ExampleRoute> { entry ->
            ExampleScreen(
                onOpenDetail = { item -> navController.navigateFrom(entry, ExampleDetailRoute(item)) },
                onBack = { navController.popBackStackFrom(entry) },
            )
        }
        composable<ExampleDetailRoute> { entry ->
            ExampleDetailScreen(onBack = { navController.popBackStackFrom(entry) })
        }
    }
}
