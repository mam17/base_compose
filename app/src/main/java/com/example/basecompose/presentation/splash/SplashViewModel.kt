package com.example.basecompose.presentation.splash

import com.example.basecompose.base.BaseViewModel
import com.example.basecompose.domain.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val settings: AppSettingsRepository,
) : BaseViewModel<SplashState, SplashEvent, SplashEffect>(SplashState) {

    init {
        launch(showLoading = false) {
            // Thời gian hiển thị tối thiểu. Load remote config / ads / dữ liệu khởi động tại đây.
            delay(SPLASH_DURATION_MILLIS)
            sendEffect(nextDestination())
        }
    }

    override fun onEvent(event: SplashEvent) = Unit

    private fun nextDestination(): SplashEffect = when {
        settings.getLanguageCode() == null -> SplashEffect.NavigateToLanguage
        !settings.isOnboardingCompleted() -> SplashEffect.NavigateToOnboarding
        else -> SplashEffect.NavigateToMain
    }

    companion object {
        const val SPLASH_DURATION_MILLIS = 1_500L
    }
}
