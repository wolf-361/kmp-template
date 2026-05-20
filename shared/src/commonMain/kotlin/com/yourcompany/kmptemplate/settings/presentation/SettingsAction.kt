package com.yourcompany.kmptemplate.settings.presentation

import com.yourcompany.kmptemplate.settings.domain.model.ThemeMode

sealed interface SettingsAction {
    data class SetThemeMode(val mode: ThemeMode) : SettingsAction
    data class SetUseDynamicColor(val enabled: Boolean) : SettingsAction
}
