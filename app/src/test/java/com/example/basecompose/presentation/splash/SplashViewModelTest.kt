package com.example.basecompose.presentation.splash

import app.cash.turbine.test
import com.example.basecompose.testutil.FakeAppSettingsRepository
import com.example.basecompose.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun runSplash(settings: FakeAppSettingsRepository, expected: SplashEffect) = runTest {
        val viewModel = SplashViewModel(settings)

        viewModel.effect.test {
            advanceTimeBy(SplashViewModel.SPLASH_DURATION_MILLIS - 1)
            expectNoEvents()

            advanceTimeBy(2)
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun `lan dau mo app - sang Language`() =
        runSplash(FakeAppSettingsRepository(), SplashEffect.NavigateToLanguage)

    @Test
    fun `da chon ngon ngu nhung chua xem onboarding - sang Onboarding`() =
        runSplash(FakeAppSettingsRepository(savedLanguageCode = "vi"), SplashEffect.NavigateToOnboarding)

    @Test
    fun `da hoan thanh onboarding - sang Main`() =
        runSplash(
            FakeAppSettingsRepository(savedLanguageCode = "vi", savedOnboardingCompleted = true),
            SplashEffect.NavigateToMain,
        )
}
