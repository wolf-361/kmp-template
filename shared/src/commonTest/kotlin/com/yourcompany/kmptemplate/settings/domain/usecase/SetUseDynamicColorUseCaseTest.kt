package com.yourcompany.kmptemplate.settings.domain.usecase

import com.yourcompany.kmptemplate.settings.FakeSettingsRepository
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SetUseDynamicColorUseCaseTest {
    private val repository = FakeSettingsRepository()
    private val useCase = SetUseDynamicColorUseCase(repository)

    @Test
    fun `delegates to repository setUseDynamicColor`() = runTest {
        useCase(true)
        repository.useDynamicColor.first() shouldBe true
    }
}
