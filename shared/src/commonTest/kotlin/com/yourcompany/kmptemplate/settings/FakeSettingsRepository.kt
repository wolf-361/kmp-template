package com.yourcompany.kmptemplate.settings

import com.yourcompany.kmptemplate.settings.domain.model.ThemeMode
import com.yourcompany.kmptemplate.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository : SettingsRepository {
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    private val _useDynamicColor = MutableStateFlow(false)

    override val themeMode: Flow<ThemeMode> = _themeMode
    override val useDynamicColor: Flow<Boolean> = _useDynamicColor

    override suspend fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }
    override suspend fun setUseDynamicColor(enabled: Boolean) {
        _useDynamicColor.value = enabled
    }
}
