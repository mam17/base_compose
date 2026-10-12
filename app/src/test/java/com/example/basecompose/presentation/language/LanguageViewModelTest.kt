package com.example.basecompose.presentation.language

import app.cash.turbine.test
import com.example.basecompose.domain.model.AppLanguage
import com.example.basecompose.testutil.FakeAppSettingsRepository
import com.example.basecompose.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class LanguageViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `chua chon ngon ngu - chon san ngon ngu cua may neu ho tro`() {
        Locale.setDefault(Locale.forLanguageTag("vi"))

        val viewModel = LanguageViewModel(FakeAppSettingsRepository())

        assertEquals(AppLanguage.VIETNAMESE, viewModel.uiState.value.selected)
    }

    @Test
    fun `ngon ngu may khong ho tro - chon ngon ngu mac dinh`() {
        Locale.setDefault(Locale.forLanguageTag("ja"))

        val viewModel = LanguageViewModel(FakeAppSettingsRepository())

        assertEquals(AppLanguage.DEFAULT, viewModel.uiState.value.selected)
    }

    @Test
    fun `da chon truoc do - chon san ngon ngu da luu`() {
        Locale.setDefault(Locale.forLanguageTag("en"))

        val viewModel = LanguageViewModel(FakeAppSettingsRepository(savedLanguageCode = "vi"))

        assertEquals(AppLanguage.VIETNAMESE, viewModel.uiState.value.selected)
    }

    @Test
    fun `confirm ngon ngu khac ngon ngu dang hien thi - luu va yeu cau recreate`() = runTest {
        Locale.setDefault(Locale.forLanguageTag("en"))
        val settings = FakeAppSettingsRepository()
        val viewModel = LanguageViewModel(settings)

        viewModel.effect.test {
            viewModel.onEvent(LanguageEvent.Select(AppLanguage.VIETNAMESE))
            viewModel.onEvent(LanguageEvent.Confirm)

            assertEquals(LanguageEffect.Applied(needRecreate = true), awaitItem())
        }
        assertEquals("vi", settings.savedLanguageCode)
    }

    @Test
    fun `confirm dung ngon ngu dang hien thi - khong can recreate`() = runTest {
        Locale.setDefault(Locale.forLanguageTag("vi"))
        val settings = FakeAppSettingsRepository()
        val viewModel = LanguageViewModel(settings)

        viewModel.effect.test {
            viewModel.onEvent(LanguageEvent.Confirm)

            assertEquals(LanguageEffect.Applied(needRecreate = false), awaitItem())
        }
        assertEquals("vi", settings.savedLanguageCode)
    }
}
