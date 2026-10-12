package com.example.basecompose.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.basecompose.di.IoDispatcher
import com.example.basecompose.domain.repository.AppSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lưu cài đặt app bằng SharedPreferences.
 *
 * Không dùng DataStore vì ngôn ngữ phải đọc ĐỒNG BỘ trong `Activity.attachBaseContext`
 * (xem [com.example.basecompose.utils.locale.AppLocale]), lúc đó chưa có Hilt / coroutine.
 */
@Singleton
class AppSettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AppSettingsRepository {

    private val prefs = prefs(context)

    override fun getLanguageCode(): String? = prefs.getString(KEY_LANGUAGE, null)

    override suspend fun setLanguageCode(code: String) = withContext(ioDispatcher) {
        prefs.edit(commit = true) { putString(KEY_LANGUAGE, code) }
    }

    override fun isOnboardingCompleted(): Boolean = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    override suspend fun setOnboardingCompleted() = withContext(ioDispatcher) {
        prefs.edit(commit = true) { putBoolean(KEY_ONBOARDING_COMPLETED, true) }
    }

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_LANGUAGE = "language_code"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

        private fun prefs(context: Context): SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        /** Đọc ngôn ngữ không cần Hilt — dùng trong `attachBaseContext`. */
        fun readLanguageCode(context: Context): String? = prefs(context).getString(KEY_LANGUAGE, null)
    }
}
