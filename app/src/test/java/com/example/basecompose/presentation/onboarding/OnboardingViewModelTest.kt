package com.example.basecompose.presentation.onboarding

import app.cash.turbine.test
import com.example.basecompose.testutil.FakeAppSettingsRepository
import com.example.basecompose.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `finish luu trang thai da xem va chuyen sang Main`() = runTest {
        val settings = FakeAppSettingsRepository(savedLanguageCode = "en")
        val viewModel = OnboardingViewModel(settings)

        viewModel.effect.test {
            viewModel.onEvent(OnboardingEvent.Finish)

            assertEquals(OnboardingEffect.NavigateToMain, awaitItem())
        }
        assertTrue(settings.savedOnboardingCompleted)
    }
}
