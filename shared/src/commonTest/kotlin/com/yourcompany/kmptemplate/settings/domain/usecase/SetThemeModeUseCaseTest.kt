package com.yourcompany.kmptemplate.settings.domain.usecase

import com.yourcompany.kmptemplate.settings.FakeSettingsRepository
import com.yourcompany.kmptemplate.settings.domain.model.ThemeMode
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SetThemeModeUseCaseTest {
    private val repository = FakeSettingsRepository()
    private val useCase = SetThemeModeUseCase(repository)

    @Test
    fun `delegates to repository setThemeMode`() = runTest {
        useCase(ThemeMode.LIGHT)
        repository.themeMode.first() shouldBe ThemeMode.LIGHT
    }
}
