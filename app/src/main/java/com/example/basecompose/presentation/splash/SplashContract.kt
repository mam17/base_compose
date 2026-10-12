package com.example.basecompose.presentation.splash

import com.example.basecompose.base.NoEvent
import com.example.basecompose.base.UiEffect
import com.example.basecompose.base.UiState

data object SplashState : UiState

typealias SplashEvent = NoEvent

sealed interface SplashEffect : UiEffect {
    data object NavigateToLanguage : SplashEffect
    data object NavigateToOnboarding : SplashEffect
    data object NavigateToMain : SplashEffect
}
