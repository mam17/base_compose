package com.example.basecompose.presentation.language

import com.example.basecompose.base.BaseViewModel
import com.example.basecompose.domain.model.AppLanguage
import com.example.basecompose.domain.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val settings: AppSettingsRepository,
) : BaseViewModel<LanguageState, LanguageEvent, LanguageEffect>(
    LanguageState(selected = initialLanguage(settings)),
) {

    override fun onEvent(event: LanguageEvent) {
        when (event) {
            is LanguageEvent.Select -> setState { copy(selected = event.language) }
            LanguageEvent.Confirm -> confirm()
        }
    }

    private fun confirm() = launch {
        val selected = currentState.selected
        val displayedCode = currentDisplayedCode(settings)
        settings.setLanguageCode(selected.code)
        sendEffect(LanguageEffect.Applied(needRecreate = selected.code != displayedCode))
    }

    private companion object {
        /** Ngôn ngữ app đang hiển thị: đã chọn trước đó, hoặc ngôn ngữ của máy. */
        fun currentDisplayedCode(settings: AppSettingsRepository): String =
            settings.getLanguageCode() ?: Locale.getDefault().language

        /** Chọn sẵn ngôn ngữ đang hiển thị nếu app hỗ trợ, ngược lại dùng mặc định. */
        fun initialLanguage(settings: AppSettingsRepository): AppLanguage =
            AppLanguage.fromCode(currentDisplayedCode(settings)) ?: AppLanguage.DEFAULT
    }
}
