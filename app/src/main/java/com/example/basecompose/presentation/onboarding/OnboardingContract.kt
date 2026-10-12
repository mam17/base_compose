package com.example.basecompose.presentation.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.basecompose.R
import com.example.basecompose.base.UiEffect
import com.example.basecompose.base.UiEvent
import com.example.basecompose.base.UiState

/** Nội dung một trang onboarding. Đổi icon/text theo app thật. */
data class OnboardingPage(
    @param:DrawableRes val image: Int,
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
)

data class OnboardingState(
    val pages: List<OnboardingPage> = DEFAULT_PAGES,
) : UiState {
    companion object {
        val DEFAULT_PAGES = listOf(
            OnboardingPage(R.drawable.ic_home, R.string.onboarding_title_1, R.string.onboarding_desc_1),
            OnboardingPage(R.drawable.ic_favorite, R.string.onboarding_title_2, R.string.onboarding_desc_2),
            OnboardingPage(R.drawable.ic_account_box, R.string.onboarding_title_3, R.string.onboarding_desc_3),
        )
    }
}

sealed interface OnboardingEvent : UiEvent {
    /** Bấm "Bỏ qua" hoặc "Bắt đầu" ở trang cuối. */
    data object Finish : OnboardingEvent
}

sealed interface OnboardingEffect : UiEffect {
    data object NavigateToMain : OnboardingEffect
}
