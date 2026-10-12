package com.example.basecompose.presentation.language

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.basecompose.R
import com.example.basecompose.base.BaseScreen
import com.example.basecompose.domain.model.AppLanguage
import com.example.basecompose.ui.theme.BaseComposeTheme

import androidx.compose.foundation.layout.height
import com.example.basecompose.ui.theme.dimens

/**
 * @param onLanguageApplied được gọi sau khi đã lưu ngôn ngữ; `needRecreate` = phải recreate Activity
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    onLanguageApplied: (needRecreate: Boolean) -> Unit,
    viewModel: LanguageViewModel = hiltViewModel(),
) {
    BaseScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is LanguageEffect.Applied -> onLanguageApplied(effect.needRecreate)
            }
        },
        topBar = { TopAppBar(title = { Text(stringResource(R.string.language_title)) }) },
        bottomBar = {
            Button(
                onClick = { viewModel.onEvent(LanguageEvent.Confirm) },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = MaterialTheme.dimens.screenPaddingHorizontal, vertical = MaterialTheme.dimens.small)
                    .height(MaterialTheme.dimens.buttonHeight),
            ) {
                Text(stringResource(R.string.common_continue))
            }
        },
    ) { state, onEvent, padding ->
        LanguageContent(state = state, onEvent = onEvent, contentPadding = padding)
    }
}

@Composable
fun LanguageContent(
    state: LanguageState,
    onEvent: (LanguageEvent) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MaterialTheme.dimens.screenPaddingHorizontal,
            end = MaterialTheme.dimens.screenPaddingHorizontal,
            top = contentPadding.calculateTopPadding() + MaterialTheme.dimens.small,
            bottom = contentPadding.calculateBottomPadding() + MaterialTheme.dimens.small,
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.itemSpacing),
    ) {
        items(state.languages, key = { it.code }) { language ->
            LanguageItem(
                language = language,
                selected = language == state.selected,
                onClick = { onEvent(LanguageEvent.Select(language)) },
            )
        }
    }
}

@Composable
private fun LanguageItem(
    language: AppLanguage,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        border = if (selected) BorderStroke(MaterialTheme.dimens.borderNormal, MaterialTheme.colorScheme.primary) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MaterialTheme.dimens.medium, vertical = MaterialTheme.dimens.space12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.medium),
        ) {
            Text(text = language.flag, style = MaterialTheme.typography.headlineMedium)
            Column(Modifier.weight(1f)) {
                Text(text = language.displayName, style = MaterialTheme.typography.titleMedium)
            }
            // onClick = null: cả Card đã xử lý click, tránh 2 vùng chạm chồng nhau
            RadioButton(selected = selected, onClick = null)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LanguageContentPreview() {
    BaseComposeTheme {
        LanguageContent(state = LanguageState(selected = AppLanguage.VIETNAMESE), onEvent = {})
    }
}
