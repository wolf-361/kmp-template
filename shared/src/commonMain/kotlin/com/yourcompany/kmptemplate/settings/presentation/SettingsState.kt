package com.yourcompany.kmptemplate.settings.presentation

import com.yourcompany.kmptemplate.settings.domain.model.ThemeMode

data class SettingsState(val themeMode: ThemeMode = ThemeMode.SYSTEM, val useDynamicColor: Boolean = false)
