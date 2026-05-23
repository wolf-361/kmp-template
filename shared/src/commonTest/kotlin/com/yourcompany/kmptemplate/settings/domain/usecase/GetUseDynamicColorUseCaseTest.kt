package com.yourcompany.kmptemplate.settings.domain.usecase

import com.yourcompany.kmptemplate.settings.FakeSettingsRepository
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class GetUseDynamicColorUseCaseTest {
    private val repository = FakeSettingsRepository()
    private val useCase = GetUseDynamicColorUseCase(repository)

    @Test
    fun `returns useDynamicColor flow from repository`() = runTest {
        repository.setUseDynamicColor(true)
        useCase().first() shouldBe true
    }
}
