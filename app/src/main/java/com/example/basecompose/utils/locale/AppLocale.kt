package com.example.basecompose.utils.locale

import android.content.Context
import android.content.res.Configuration
import com.example.basecompose.data.local.AppSettingsRepositoryImpl
import java.util.Locale

/**
 * Áp dụng ngôn ngữ người dùng đã chọn cho Activity (chạy từ API 24, không cần AppCompat).
 *
 * Activity gọi trong `attachBaseContext`:
 * ```kotlin
 * override fun attachBaseContext(newBase: Context) {
 *     super.attachBaseContext(AppLocale.wrap(newBase))
 * }
 * ```
 * Sau khi người dùng đổi ngôn ngữ: lưu lại rồi gọi `activity.recreate()`.
 */
object AppLocale {

    fun wrap(base: Context): Context {
        val code = AppSettingsRepositoryImpl.readLanguageCode(base) ?: return base
        val locale = Locale.forLanguageTag(code)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(config)
    }
}
