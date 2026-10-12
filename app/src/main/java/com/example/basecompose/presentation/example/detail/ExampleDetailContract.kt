package com.example.basecompose.presentation.example.detail

import com.example.basecompose.base.UiEffect
import com.example.basecompose.base.UiEvent
import com.example.basecompose.base.UiState
import kotlinx.serialization.Serializable

/** Route có tham số: Navigation tự serialize, ViewModel đọc lại qua `savedStateHandle.toRoute()`. */
@Serializable
data class ExampleDetailRoute(val item: String)

data class ExampleDetailState(
    val item: String = "",
) : UiState

sealed interface ExampleDetailEvent : UiEvent {
    data object Back : ExampleDetailEvent
}

sealed interface ExampleDetailEffect : UiEffect {
    data object NavigateBack : ExampleDetailEffect
}
