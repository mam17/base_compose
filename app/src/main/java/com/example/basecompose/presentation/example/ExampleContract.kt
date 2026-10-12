package com.example.basecompose.presentation.example

import com.example.basecompose.base.LoadState
import com.example.basecompose.base.UiEffect
import com.example.basecompose.base.UiEvent
import com.example.basecompose.base.UiState

/** Template contract cho một màn: copy file này khi tạo màn mới. */
data class ExampleState(
    val query: String = "",
    val items: LoadState<List<String>> = LoadState.Idle,
) : UiState

sealed interface ExampleEvent : UiEvent {
    data object Refresh : ExampleEvent
    data class QueryChanged(val value: String) : ExampleEvent
    data class ItemClicked(val item: String) : ExampleEvent
    data object Back : ExampleEvent
}

sealed interface ExampleEffect : UiEffect {
    data class OpenDetail(val item: String) : ExampleEffect
    data object NavigateBack : ExampleEffect
}
