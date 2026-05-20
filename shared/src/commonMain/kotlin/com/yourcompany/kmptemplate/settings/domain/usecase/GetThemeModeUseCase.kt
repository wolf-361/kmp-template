package com.yourcompany.kmptemplate.settings.domain.usecase

import com.yourcompany.kmptemplate.settings.domain.model.ThemeMode
import com.yourcompany.kmptemplate.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

open class GetThemeModeUseCase(private val repository: SettingsRepository) {
    open operator fun invoke(): Flow<ThemeMode> = repository.themeMode
}
