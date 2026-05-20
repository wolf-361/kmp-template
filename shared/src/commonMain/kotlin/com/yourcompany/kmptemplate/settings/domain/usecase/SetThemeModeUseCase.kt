package com.yourcompany.kmptemplate.settings.domain.usecase

import com.yourcompany.kmptemplate.settings.domain.model.ThemeMode
import com.yourcompany.kmptemplate.settings.domain.repository.SettingsRepository

open class SetThemeModeUseCase(private val repository: SettingsRepository) {
    open suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}
