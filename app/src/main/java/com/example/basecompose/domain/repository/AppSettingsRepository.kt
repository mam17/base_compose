package com.example.basecompose.domain.repository

/** Cài đặt cấp app: ngôn ngữ đã chọn, đã xem onboarding chưa. */
interface AppSettingsRepository {

    /** Mã ngôn ngữ người dùng đã chọn (vd "vi", "en"), null nếu chưa chọn lần nào. */
    fun getLanguageCode(): String?

    suspend fun setLanguageCode(code: String)

    fun isOnboardingCompleted(): Boolean

    suspend fun setOnboardingCompleted()
}
