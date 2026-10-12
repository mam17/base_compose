package com.example.basecompose.presentation.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.basecompose.R
import com.example.basecompose.base.BaseScreen
import com.example.basecompose.ui.theme.BaseComposeTheme
import com.example.basecompose.ui.theme.dimens
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onNavigateToMain: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    BaseScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                OnboardingEffect.NavigateToMain -> onNavigateToMain()
            }
        },
    ) { state, onEvent, padding ->
        OnboardingContent(state = state, onEvent = onEvent, contentPadding = padding)
    }
}

@Composable
fun OnboardingContent(
    state: OnboardingState,
    onEvent: (OnboardingEvent) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    pagerState: PagerState = rememberPagerState { state.pages.size },
) {
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == state.pages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        // Nút "Bỏ qua" ẩn ở trang cuối nhưng vẫn giữ chỗ để layout không nhảy
        Box(Modifier.fillMaxWidth().height(MaterialTheme.dimens.minTouchTarget)) {
            if (!isLastPage) {
                TextButton(
                    onClick = { onEvent(OnboardingEvent.Finish) },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = MaterialTheme.dimens.small),
                ) {
                    Text(stringResource(R.string.onboarding_skip))
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { index ->
            OnboardingPageContent(state.pages[index])
        }

        PageIndicator(
            pageCount = state.pages.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = MaterialTheme.dimens.large),
        )

        Button(
            onClick = {
                if (isLastPage) {
                    onEvent(OnboardingEvent.Finish)
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = MaterialTheme.dimens.screenPaddingHorizontal,
                    end = MaterialTheme.dimens.screenPaddingHorizontal,
                    bottom = MaterialTheme.dimens.screenPaddingVertical
                )
                .height(MaterialTheme.dimens.buttonHeight),
        ) {
            Text(stringResource(if (isLastPage) R.string.onboarding_get_started else R.string.onboarding_next))
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.dimens.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val imageContainerSize = if (MaterialTheme.dimens.isCompact) 180.dp else 240.dp
        Box(
            modifier = Modifier
                .size(imageContainerSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(page.image),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer),
                modifier = Modifier.size(MaterialTheme.dimens.space96),
            )
        }
        Spacer(Modifier.height(MaterialTheme.dimens.space40))
        Text(
            text = stringResource(page.title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(MaterialTheme.dimens.space12))
        Text(
            text = stringResource(page.description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.small)) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(
                if (selected) MaterialTheme.dimens.large else MaterialTheme.dimens.small,
                label = "indicatorWidth"
            )
            val color by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                label = "indicatorColor",
            )
            Box(
                Modifier
                    .size(width = width, height = MaterialTheme.dimens.small)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingContentPreview() {
    BaseComposeTheme {
        OnboardingContent(state = OnboardingState(), onEvent = {})
    }
}
