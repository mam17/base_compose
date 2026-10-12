package com.example.basecompose.testutil

import com.example.basecompose.domain.repository.AppSettingsRepository

class FakeAppSettingsRepository(
    var savedLanguageCode: String? = null,
    var savedOnboardingCompleted: Boolean = false,
) : AppSettingsRepository {

    override fun getLanguageCode(): String? = savedLanguageCode

    override suspend fun setLanguageCode(code: String) {
        savedLanguageCode = code
    }

    override fun isOnboardingCompleted(): Boolean = savedOnboardingCompleted

    override suspend fun setOnboardingCompleted() {
        savedOnboardingCompleted = true
    }
}
