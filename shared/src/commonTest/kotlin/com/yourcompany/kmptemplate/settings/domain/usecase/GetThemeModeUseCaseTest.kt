package com.yourcompany.kmptemplate.settings.domain.usecase

import com.yourcompany.kmptemplate.settings.FakeSettingsRepository
import com.yourcompany.kmptemplate.settings.domain.model.ThemeMode
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class GetThemeModeUseCaseTest {
    private val repository = FakeSettingsRepository()
    private val useCase = GetThemeModeUseCase(repository)

    @Test
    fun `returns themeMode flow from repository`() = runTest {
        repository.setThemeMode(ThemeMode.DARK)
        useCase().first() shouldBe ThemeMode.DARK
    }
}
