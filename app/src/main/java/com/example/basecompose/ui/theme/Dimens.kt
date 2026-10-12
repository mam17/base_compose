package com.example.basecompose.ui.theme

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Phân loại kích thước màn hình theo chiều rộng (Tuân theo chuẩn Material 3 WindowSizeClass).
 */
enum class WindowWidthSizeClass {
    /** Màn hình hẹp / nhỏ (< 360dp): Màn hình phụ điện thoại gập (Flip cover screen), máy nhỏ */
    COMPACT_SMALL,

    /** Màn hình điện thoại tiêu chuẩn (360dp .. 599dp): Đa số smartphone hiện nay */
    COMPACT,

    /** Màn hình trung bình (600dp .. 839dp): Điện thoại gập mở (Foldable unfolded), tablet mini */
    MEDIUM,

    /** Màn hình lớn (>= 840dp): Máy tính bảng lớn, máy tính bảng xoay ngang, desktop */
    EXPANDED
}

/**
 * Phân loại kích thước màn hình theo chiều cao.
 */
enum class WindowHeightSizeClass {
    /** Chiều cao bị giới hạn (< 480dp): Điện thoại xoay ngang (Landscape) */
    COMPACT,

    /** Chiều cao tiêu chuẩn (480dp .. 899dp) */
    MEDIUM,

    /** Chiều cao lớn (>= 900dp): Máy tính bảng đặt dọc */
    EXPANDED
}

/**
 * Thông tin chi tiết về màn hình hiện tại để hỗ trợ responsive/adaptive layout.
 */
data class ScreenSpec(
    val widthDp: Dp,
    val heightDp: Dp,
    val widthSizeClass: WindowWidthSizeClass,
    val heightSizeClass: WindowHeightSizeClass,
    val isLandscape: Boolean,
    val isTabletOrFoldable: Boolean,
)

/**
 * Hệ thống Dimension & Spacing chuẩn cho Jetpack Compose.
 *
 * Tự động thích ứng (Adaptive) với:
 * - Điện thoại nhỏ / màn hình phụ (< 360dp)
 * - Điện thoại thông thường (360dp - 599dp)
 * - Điện thoại gập mở / máy tính bảng nhỏ (600dp - 839dp)
 * - Máy tính bảng lớn / Desktop (>= 840dp)
 * - Chế độ xoay ngang (Landscape mode)
 */
data class Dimensions(
    val screen: ScreenSpec,

    // --- Semantic Spacing (Material 3 standard) ---
    val none: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val extraExtraLarge: Dp = 48.dp,
    val huge: Dp = 64.dp,

    // --- Step Grid Spacing (bội số của 4dp/8dp grid) ---
    val space2: Dp = 2.dp,
    val space4: Dp = 4.dp,
    val space6: Dp = 6.dp,
    val space8: Dp = 8.dp,
    val space10: Dp = 10.dp,
    val space12: Dp = 12.dp,
    val space14: Dp = 14.dp,
    val space16: Dp = 16.dp,
    val space20: Dp = 20.dp,
    val space24: Dp = 24.dp,
    val space28: Dp = 28.dp,
    val space32: Dp = 32.dp,
    val space36: Dp = 36.dp,
    val space40: Dp = 40.dp,
    val space48: Dp = 48.dp,
    val space56: Dp = 56.dp,
    val space64: Dp = 64.dp,
    val space72: Dp = 72.dp,
    val space80: Dp = 80.dp,
    val space96: Dp = 96.dp,

    // --- Layout & Screen Adapters ---
    val screenPaddingHorizontal: Dp,
    val screenPaddingVertical: Dp,
    val screenPadding: Dp,
    val contentPadding: Dp,
    val itemSpacing: Dp,
    val sectionSpacing: Dp,
    val maxContentWidth: Dp,
    val dialogMaxWidth: Dp,
    val dialogPadding: Dp,
    val bottomSheetMaxWidth: Dp,

    // --- Components & Touch Targets ---
    val minTouchTarget: Dp = 48.dp, // Tiêu chuẩn tiếp cận Accessibility của Google
    val buttonHeightSmall: Dp = 36.dp,
    val buttonHeight: Dp = 48.dp,
    val buttonHeightLarge: Dp = 56.dp,
    val topAppBarHeight: Dp = 56.dp,
    val bottomBarHeight: Dp = 64.dp,
    val cardMinHeight: Dp = 64.dp,

    // --- Icon Sizes ---
    val iconExtraSmall: Dp = 16.dp,
    val iconSmall: Dp = 20.dp,
    val iconMedium: Dp = 24.dp,
    val iconLarge: Dp = 32.dp,
    val iconExtraLarge: Dp = 48.dp,

    // --- Corner Radii ---
    val radiusExtraSmall: Dp = 4.dp,
    val radiusSmall: Dp = 8.dp,
    val radiusMedium: Dp = 12.dp,
    val radiusLarge: Dp = 16.dp,
    val radiusExtraLarge: Dp = 24.dp,
    val radiusFull: Dp = 999.dp,

    // --- Elevation ---
    val elevationNone: Dp = 0.dp,
    val elevationSmall: Dp = 2.dp,
    val elevationMedium: Dp = 4.dp,
    val elevationLarge: Dp = 8.dp,
    val elevationExtraLarge: Dp = 16.dp,

    // --- Border Widths ---
    val borderThin: Dp = 1.dp,
    val borderNormal: Dp = 2.dp,
    val borderThick: Dp = 3.dp,
) {
    /** Đang chạy trên điện thoại (Compact) */
    val isCompact: Boolean
        get() = screen.widthSizeClass == WindowWidthSizeClass.COMPACT || screen.widthSizeClass == WindowWidthSizeClass.COMPACT_SMALL

    /** Đang chạy trên máy tính bảng hoặc màn hình gập mở rộng */
    val isTablet: Boolean
        get() = screen.isTabletOrFoldable

    /** Đang ở chế độ màn hình ngang */
    val isLandscape: Boolean
        get() = screen.isLandscape

    /**
     * Scale một kích thước Dp theo tỉ lệ màn hình so với chuẩn 360dp.
     * Cực kỳ hữu ích cho Game canvas hoặc các UI custom cần co giãn theo tỉ lệ màn hình.
     */
    fun scaleDp(baseDp: Dp): Dp {
        val factor = (screen.widthDp.value / 360f).coerceIn(0.85f, 1.8f)
        return baseDp * factor
    }
}

/**
 * Tạo bộ Dimensions tối ưu hóa cho từng cấu hình màn hình.
 */
fun createDimensions(
    widthDp: Dp,
    heightDp: Dp,
    isLandscape: Boolean
): Dimensions {
    val widthClass = when {
        widthDp < 360.dp -> WindowWidthSizeClass.COMPACT_SMALL
        widthDp < 600.dp -> WindowWidthSizeClass.COMPACT
        widthDp < 840.dp -> WindowWidthSizeClass.MEDIUM
        else -> WindowWidthSizeClass.EXPANDED
    }

    val heightClass = when {
        heightDp < 480.dp -> WindowHeightSizeClass.COMPACT
        heightDp < 900.dp -> WindowHeightSizeClass.MEDIUM
        else -> WindowHeightSizeClass.EXPANDED
    }

    val isTabletOrFoldable = widthClass == WindowWidthSizeClass.MEDIUM || widthClass == WindowWidthSizeClass.EXPANDED

    val screenSpec = ScreenSpec(
        widthDp = widthDp,
        heightDp = heightDp,
        widthSizeClass = widthClass,
        heightSizeClass = heightClass,
        isLandscape = isLandscape,
        isTabletOrFoldable = isTabletOrFoldable,
    )

    return when (widthClass) {
        WindowWidthSizeClass.COMPACT_SMALL -> {
            // Màn hình rất hẹp / nhỏ (như Galaxy Flip cover screen, điện thoại nhỏ < 360dp)
            Dimensions(
                screen = screenSpec,
                screenPaddingHorizontal = 12.dp,
                screenPaddingVertical = 12.dp,
                screenPadding = 12.dp,
                contentPadding = 12.dp,
                itemSpacing = 8.dp,
                sectionSpacing = 16.dp,
                maxContentWidth = 360.dp,
                dialogMaxWidth = 300.dp,
                dialogPadding = 16.dp,
                bottomSheetMaxWidth = 340.dp,
                buttonHeight = 44.dp,
                topAppBarHeight = 48.dp,
                bottomBarHeight = 56.dp,
            )
        }

        WindowWidthSizeClass.COMPACT -> {
            // Điện thoại thông thường (360dp .. 599dp)
            val vPadding = if (isLandscape) 8.dp else 16.dp
            val topBarH = if (isLandscape) 48.dp else 56.dp
            Dimensions(
                screen = screenSpec,
                screenPaddingHorizontal = 16.dp,
                screenPaddingVertical = vPadding,
                screenPadding = 16.dp,
                contentPadding = 16.dp,
                itemSpacing = 12.dp,
                sectionSpacing = if (isLandscape) 14.dp else 24.dp,
                maxContentWidth = 480.dp,
                dialogMaxWidth = 360.dp,
                dialogPadding = 22.dp,
                bottomSheetMaxWidth = 440.dp,
                buttonHeight = 48.dp,
                topAppBarHeight = topBarH,
                bottomBarHeight = 64.dp,
            )
        }

        WindowWidthSizeClass.MEDIUM -> {
            // Điện thoại gập mở ra (Foldables: Fold 5/6, Pixel Fold) & Tablet mini (600dp .. 839dp)
            val vPadding = if (isLandscape) 16.dp else 24.dp
            Dimensions(
                screen = screenSpec,
                screenPaddingHorizontal = 24.dp,
                screenPaddingVertical = vPadding,
                screenPadding = 24.dp,
                contentPadding = 20.dp,
                itemSpacing = 16.dp,
                sectionSpacing = 28.dp,
                maxContentWidth = 640.dp,
                dialogMaxWidth = 480.dp,
                dialogPadding = 28.dp,
                bottomSheetMaxWidth = 540.dp,
                buttonHeight = 52.dp,
                topAppBarHeight = 64.dp,
                bottomBarHeight = 72.dp,
            )
        }

        WindowWidthSizeClass.EXPANDED -> {
            // Máy tính bảng lớn (>= 840dp, Tab S9, iPad Pro form factor, Desktop)
            Dimensions(
                screen = screenSpec,
                screenPaddingHorizontal = 32.dp,
                screenPaddingVertical = 24.dp,
                screenPadding = 32.dp,
                contentPadding = 24.dp,
                itemSpacing = 20.dp,
                sectionSpacing = 36.dp,
                maxContentWidth = 840.dp,
                dialogMaxWidth = 560.dp,
                dialogPadding = 32.dp,
                bottomSheetMaxWidth = 640.dp,
                buttonHeight = 56.dp,
                topAppBarHeight = 64.dp,
                bottomBarHeight = 72.dp,
            )
        }
    }
}

/**
 * Dimens mặc định khi khởi tạo ngoài Composable context.
 */
val DefaultDimensions: Dimensions = createDimensions(
    widthDp = 392.dp,
    heightDp = 800.dp,
    isLandscape = false
)

/**
 * CompositionLocal cung cấp [Dimensions] xuyên suốt cây giao diện Compose.
 */
val LocalDimensions = staticCompositionLocalOf { DefaultDimensions }

/**
 * Alias cho `LocalDimensions` để thuận tiện sử dụng.
 */
val LocalDimens = LocalDimensions

/**
 * Tự động tính toán dimensions phù hợp dựa theo kích thước và định hướng màn hình hiện tại.
 * Tự cập nhật khi người dùng xoay ngang màn hình hoặc gập/mở thiết bị gập (Foldables).
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun rememberDimensions(): Dimensions {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || screenHeightDp < screenWidthDp

    return remember(screenWidthDp, screenHeightDp, isLandscape) {
        createDimensions(
            widthDp = screenWidthDp,
            heightDp = screenHeightDp,
            isLandscape = isLandscape
        )
    }
}

/**
 * Truy cập [Dimensions] từ [MaterialTheme]:
 * ```kotlin
 * val padding = MaterialTheme.dimens.screenPadding
 * val isTablet = MaterialTheme.dimens.isTablet
 * ```
 */
val MaterialTheme.dimens: Dimensions
    @Composable
    @ReadOnlyComposable
    get() = LocalDimensions.current

/**
 * Alias cho [MaterialTheme.dimens], trực quan khi làm việc với khoảng cách (Spacing):
 * ```kotlin
 * val spacing = MaterialTheme.spacing.space16
 * val gap = MaterialTheme.spacing.medium
 * ```
 */
val MaterialTheme.spacing: Dimensions
    @Composable
    @ReadOnlyComposable
    get() = LocalDimensions.current
