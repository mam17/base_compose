package com.example.basecompose.base

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Điều khiển system bars (status bar / navigation bar) cho MỘT màn hình.
 * Khi màn rời khỏi composition, các thay đổi được hoàn tác.
 *
 * Thay thế các hàm setFullScreen / hideStatusBar / setStatusBarIconsColor... của BaseActivity (View system):
 * trong Compose, cấu hình này thuộc về từng màn chứ không thuộc Activity.
 *
 * Lưu ý: từ Android 15 app luôn edge-to-edge, màu nền system bar không còn đổi được —
 * hãy vẽ nền phía sau bar bằng Compose thay vì set `window.statusBarColor`.
 *
 * ```kotlin
 * @Composable
 * fun VideoPlayerRoute() {
 *     SystemBarsEffect(hideStatusBar = true, hideNavigationBar = true)
 *     ...
 * }
 * ```
 *
 * @param hideStatusBar ẩn status bar
 * @param hideNavigationBar ẩn navigation bar
 * @param lightStatusBarIcons true = icon tối (cho nền sáng), false = icon sáng, null = giữ nguyên
 * @param lightNavigationBarIcons tương tự cho navigation bar
 */
@Composable
fun SystemBarsEffect(
    hideStatusBar: Boolean = false,
    hideNavigationBar: Boolean = false,
    lightStatusBarIcons: Boolean? = null,
    lightNavigationBarIcons: Boolean? = null,
) {
    val view = LocalView.current
    if (view.isInEditMode) return

    DisposableEffect(hideStatusBar, hideNavigationBar, lightStatusBarIcons, lightNavigationBarIcons) {
        val window = view.context.findActivity()?.window ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        val oldLightStatus = controller.isAppearanceLightStatusBars
        val oldLightNav = controller.isAppearanceLightNavigationBars

        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (hideStatusBar) controller.hide(WindowInsetsCompat.Type.statusBars())
        if (hideNavigationBar) controller.hide(WindowInsetsCompat.Type.navigationBars())
        lightStatusBarIcons?.let { controller.isAppearanceLightStatusBars = it }
        lightNavigationBarIcons?.let { controller.isAppearanceLightNavigationBars = it }

        onDispose {
            if (hideStatusBar) controller.show(WindowInsetsCompat.Type.statusBars())
            if (hideNavigationBar) controller.show(WindowInsetsCompat.Type.navigationBars())
            controller.isAppearanceLightStatusBars = oldLightStatus
            controller.isAppearanceLightNavigationBars = oldLightNav
        }
    }
}

/** Tìm Activity từ Context (Context trong Compose có thể bị bọc bởi ContextWrapper). */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
