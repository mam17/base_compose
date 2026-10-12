package com.example.basecompose.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

/**
 * Các helper điều hướng dùng chung. Route là class/object `@Serializable` (type-safe),
 * xem [AppNavHost].
 */

/**
 * true nếu màn [this] đang RESUMED, tức đang hiển thị và không nằm giữa animation chuyển màn.
 * Dùng để chặn việc navigate 2 lần khi người dùng bấm liên tục.
 */
val NavBackStackEntry.isResumed: Boolean
    get() = lifecycle.currentState == Lifecycle.State.RESUMED

/**
 * Navigate từ màn [from] tới [route], bỏ qua nếu [from] không còn RESUMED
 * (đang chuyển màn / đã bị đóng) -> không bao giờ mở trùng màn.
 *
 * ```kotlin
 * composable<HomeRoute> { entry ->
 *     HomeRoute(onOpenDetail = { id -> navController.navigateFrom(entry, DetailRoute(id)) })
 * }
 * ```
 */
fun <T : Any> NavController.navigateFrom(
    from: NavBackStackEntry,
    route: T,
    builder: NavOptionsBuilder.() -> Unit = {},
) {
    if (from.isResumed) navigate(route, builder)
}

/**
 * Quay lại màn trước, bỏ qua nếu [from] không còn RESUMED.
 * Tránh trường hợp bấm back 2 lần làm pop luôn màn phía dưới (màn trắng).
 */
fun NavController.popBackStackFrom(from: NavBackStackEntry) {
    if (from.isResumed) popBackStack()
}

/**
 * Mở [route] và xoá toàn bộ back stack phía sau (vd: Login -> Home, Logout -> Login).
 */
fun <T : Any> NavController.navigateAndClearBackStack(route: T) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
