package com.example.basecompose.presentation.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.basecompose.base.BaseScreen
import com.example.basecompose.base.LoadState
import com.example.basecompose.ui.theme.BaseComposeTheme
import com.example.basecompose.ui.theme.dimens
import com.example.basecompose.utils.extensions.debouncedClickable

/**
 * Screen (stateful): nối ViewModel với [BaseScreen], xử lý effect / navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExampleScreen(
    onOpenDetail: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ExampleViewModel = hiltViewModel(),
) {
    BaseScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is ExampleEffect.OpenDetail -> onOpenDetail(effect.item)
                ExampleEffect.NavigateBack -> onBack()
            }
        },
        topBar = { TopAppBar(title = { Text("Example") }) },
    ) { state, onEvent, padding ->
        ExampleContent(state = state, onEvent = onEvent, contentPadding = padding)
    }
}

/**
 * Content (stateless): chỉ phụ thuộc state + onEvent -> Preview / UI test dễ dàng.
 */
@Composable
fun ExampleContent(
    state: ExampleState,
    onEvent: (ExampleEvent) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = { onEvent(ExampleEvent.QueryChanged(it)) },
            label = { Text("Tìm kiếm") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.dimens.screenPaddingHorizontal, vertical = MaterialTheme.dimens.small),
        )

        when (val items = state.items) {
            LoadState.Idle -> Unit
            LoadState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is LoadState.Error -> Text(
                text = items.message.asString(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(MaterialTheme.dimens.screenPadding),
            )
            is LoadState.Success -> LazyColumn {
                items(items.data, key = { it }) { item ->
                    ListItem(
                        headlineContent = { Text(item) },
                        modifier = Modifier.debouncedClickable { onEvent(ExampleEvent.ItemClicked(item)) },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExampleContentPreview() {
    BaseComposeTheme {
        ExampleContent(
            state = ExampleState(items = LoadState.Success(listOf("Item 1", "Item 2"))),
            onEvent = {},
        )
    }
}
