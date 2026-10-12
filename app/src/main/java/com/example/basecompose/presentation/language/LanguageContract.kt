package com.example.basecompose.presentation.language

import com.example.basecompose.base.UiEffect
import com.example.basecompose.base.UiEvent
import com.example.basecompose.base.UiState
import com.example.basecompose.domain.model.AppLanguage

data class LanguageState(
    val languages: List<AppLanguage> = AppLanguage.entries,
    val selected: AppLanguage = AppLanguage.DEFAULT,
) : UiState

sealed interface LanguageEvent : UiEvent {
    data class Select(val language: AppLanguage) : LanguageEvent
    data object Confirm : LanguageEvent
}

sealed interface LanguageEffect : UiEffect {
    /**
     * Đã lưu ngôn ngữ, đi tiếp sang màn sau.
     * @property needRecreate ngôn ngữ khác với ngôn ngữ đang hiển thị -> phải recreate Activity
     */
    data class Applied(val needRecreate: Boolean) : LanguageEffect
}
