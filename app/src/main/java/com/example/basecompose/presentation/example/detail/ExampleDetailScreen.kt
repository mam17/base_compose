package com.example.basecompose.presentation.example.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.basecompose.base.BaseScreen
import com.example.basecompose.ui.theme.BaseComposeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExampleDetailScreen(
    onBack: () -> Unit,
    viewModel: ExampleDetailViewModel = hiltViewModel(),
) {
    BaseScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                ExampleDetailEffect.NavigateBack -> onBack()
            }
        },
        topBar = { TopAppBar(title = { Text("Chi tiết") }) },
    ) { state, onEvent, padding ->
        ExampleDetailContent(state = state, onEvent = onEvent, contentPadding = padding)
    }
}

@Composable
fun ExampleDetailContent(
    state: ExampleDetailState,
    onEvent: (ExampleDetailEvent) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = state.item, style = MaterialTheme.typography.headlineMedium)
        Button(onClick = { onEvent(ExampleDetailEvent.Back) }) {
            Text("Quay lại")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExampleDetailContentPreview() {
    BaseComposeTheme {
        ExampleDetailContent(state = ExampleDetailState(item = "Item 1"), onEvent = {})
    }
}
