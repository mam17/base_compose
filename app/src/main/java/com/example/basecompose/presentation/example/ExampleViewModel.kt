package com.example.basecompose.presentation.example

import com.example.basecompose.base.BaseViewModel
import com.example.basecompose.base.LoadState
import com.example.basecompose.base.UiText
import com.example.basecompose.base.loadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class ExampleViewModel @Inject constructor(
    // Inject UseCase / Repository tại đây
) : BaseViewModel<ExampleState, ExampleEvent, ExampleEffect>(ExampleState()) {

    init {
        onEvent(ExampleEvent.Refresh)
    }

    override fun onEvent(event: ExampleEvent) {
        when (event) {
            ExampleEvent.Refresh -> refresh()
            is ExampleEvent.QueryChanged -> search(event.value)
            is ExampleEvent.ItemClicked -> sendEffect(ExampleEffect.OpenDetail(event.item))
            ExampleEvent.Back -> sendEffect(ExampleEffect.NavigateBack)
        }
    }

    /** Loading toàn màn (overlay) + lỗi tự hiện dialog nhờ [BaseViewModel.launch]. */
    private fun refresh() = launch {
        val items = fakeFetch(currentState.query)
        setState { copy(items = LoadState.Success(items)) }
        showMessage(UiText("Đã tải ${items.size} mục"))
    }

    /** Loading cục bộ trong state + huỷ request cũ khi người dùng gõ tiếp. */
    private fun search(query: String) {
        setState { copy(query = query) }
        launchLatest(key = "search", showLoading = false) {
            delay(300) // debounce
            setState { copy(items = LoadState.Loading) }
            val result = loadState { fakeFetch(query) }
            setState { copy(items = result) }
        }
    }

    private suspend fun fakeFetch(query: String): List<String> {
        delay(500)
        return (1..20).map { "Item $it" }.filter { it.contains(query, ignoreCase = true) }
    }
}
