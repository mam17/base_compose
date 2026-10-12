package com.example.basecompose.presentation.onboarding

import com.example.basecompose.base.BaseViewModel
import com.example.basecompose.domain.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Trang hiện tại do PagerState ở UI quản lý (thuần UI, không cần qua ViewModel).
 * ViewModel chỉ lo việc có logic: lưu "đã xem onboarding" rồi chuyển sang Main.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: AppSettingsRepository,
) : BaseViewModel<OnboardingState, OnboardingEvent, OnboardingEffect>(OnboardingState()) {

    override fun onEvent(event: OnboardingEvent) {
        when (event) {
            OnboardingEvent.Finish -> finish()
        }
    }

    private fun finish() = launch {
        settings.setOnboardingCompleted()
        sendEffect(OnboardingEffect.NavigateToMain)
    }
}
