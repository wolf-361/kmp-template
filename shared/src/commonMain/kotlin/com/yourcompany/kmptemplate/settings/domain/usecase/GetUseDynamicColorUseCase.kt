package com.yourcompany.kmptemplate.settings.domain.usecase

import com.yourcompany.kmptemplate.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

open class GetUseDynamicColorUseCase(private val repository: SettingsRepository) {
    open operator fun invoke(): Flow<Boolean> = repository.useDynamicColor
}
