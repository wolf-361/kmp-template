package com.yourcompany.kmptemplate.settings.presentation

import com.yourcompany.kmptemplate.core.presentation.BaseViewModel
import com.yourcompany.kmptemplate.settings.domain.usecase.GetThemeModeUseCase
import com.yourcompany.kmptemplate.settings.domain.usecase.GetUseDynamicColorUseCase
import com.yourcompany.kmptemplate.settings.domain.usecase.SetThemeModeUseCase
import com.yourcompany.kmptemplate.settings.domain.usecase.SetUseDynamicColorUseCase
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val getThemeMode: GetThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val getUseDynamicColor: GetUseDynamicColorUseCase,
    private val setUseDynamicColor: SetUseDynamicColorUseCase,
) : BaseViewModel<SettingsState, SettingsAction, Nothing>(SettingsState()) {

    init {
        viewModelScope.launch {
            getThemeMode().collect { setState { copy(themeMode = it) } }
        }
        viewModelScope.launch {
            getUseDynamicColor().collect { setState { copy(useDynamicColor = it) } }
        }
    }

    override fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SetThemeMode ->
                viewModelScope.launch { setThemeMode(action.mode) }
            is SettingsAction.SetUseDynamicColor ->
                viewModelScope.launch { setUseDynamicColor(action.enabled) }
        }
    }
}
