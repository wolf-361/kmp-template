package com.yourcompany.kmptemplate.settings.domain.usecase

import com.yourcompany.kmptemplate.settings.domain.repository.SettingsRepository

open class SetUseDynamicColorUseCase(private val repository: SettingsRepository) {
    open suspend operator fun invoke(enabled: Boolean) = repository.setUseDynamicColor(enabled)
}
