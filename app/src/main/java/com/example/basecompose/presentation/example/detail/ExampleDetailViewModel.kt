package com.example.basecompose.presentation.example.detail

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.example.basecompose.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ExampleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<ExampleDetailState, ExampleDetailEvent, ExampleDetailEffect>(
    ExampleDetailState(item = savedStateHandle.toRoute<ExampleDetailRoute>().item),
) {

    override fun onEvent(event: ExampleDetailEvent) {
        when (event) {
            ExampleDetailEvent.Back -> sendEffect(ExampleDetailEffect.NavigateBack)
        }
    }
}
