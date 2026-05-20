package com.yourcompany.kmptemplate.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.yourcompany.kmptemplate.settings.data.repository.SettingsRepositoryImpl
import com.yourcompany.kmptemplate.settings.domain.repository.SettingsRepository
import com.yourcompany.kmptemplate.settings.domain.usecase.GetThemeModeUseCase
import com.yourcompany.kmptemplate.settings.domain.usecase.GetUseDynamicColorUseCase
import com.yourcompany.kmptemplate.settings.domain.usecase.SetThemeModeUseCase
import com.yourcompany.kmptemplate.settings.domain.usecase.SetUseDynamicColorUseCase
import com.yourcompany.kmptemplate.settings.presentation.SettingsViewModel
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class SettingsModule {

    @Single
    fun provideSettingsRepository(dataStore: DataStore<Preferences>): SettingsRepository =
        SettingsRepositoryImpl(dataStore)

    @Factory
    fun provideGetThemeModeUseCase(repository: SettingsRepository): GetThemeModeUseCase =
        GetThemeModeUseCase(repository)

    @Factory
    fun provideSetThemeModeUseCase(repository: SettingsRepository): SetThemeModeUseCase =
        SetThemeModeUseCase(repository)

    @Factory
    fun provideGetUseDynamicColorUseCase(repository: SettingsRepository): GetUseDynamicColorUseCase =
        GetUseDynamicColorUseCase(repository)

    @Factory
    fun provideSetUseDynamicColorUseCase(repository: SettingsRepository): SetUseDynamicColorUseCase =
        SetUseDynamicColorUseCase(repository)

    @Factory
    fun provideSettingsViewModel(
        getThemeMode: GetThemeModeUseCase,
        setThemeMode: SetThemeModeUseCase,
        getUseDynamicColor: GetUseDynamicColorUseCase,
        setUseDynamicColor: SetUseDynamicColorUseCase,
    ): SettingsViewModel = SettingsViewModel(getThemeMode, setThemeMode, getUseDynamicColor, setUseDynamicColor)
}
