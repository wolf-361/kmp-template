package com.yourcompany.kmptemplate.settings.presentation

import com.yourcompany.kmptemplate.core.data.network.GlobalUiEffectsHandler
import com.yourcompany.kmptemplate.core.test.BaseViewModelTest
import com.yourcompany.kmptemplate.core.test.startTestKoin
import com.yourcompany.kmptemplate.core.test.stopTestKoin
import com.yourcompany.kmptemplate.settings.domain.model.ThemeMode
import com.yourcompany.kmptemplate.settings.domain.usecase.GetThemeModeUseCase
import com.yourcompany.kmptemplate.settings.domain.usecase.GetUseDynamicColorUseCase
import com.yourcompany.kmptemplate.settings.domain.usecase.SetThemeModeUseCase
import com.yourcompany.kmptemplate.settings.domain.usecase.SetUseDynamicColorUseCase
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest : BaseViewModelTest() {

    private val getThemeMode = mock<GetThemeModeUseCase>()
    private val setThemeMode = mock<SetThemeModeUseCase>()
    private val getUseDynamicColor = mock<GetUseDynamicColorUseCase>()
    private val setUseDynamicColor = mock<SetUseDynamicColorUseCase>()
    private val globalUiEffectsHandler = GlobalUiEffectsHandler()
    private lateinit var viewModel: SettingsViewModel

    @BeforeTest
    fun setUp() {
        startTestKoin(module { single { globalUiEffectsHandler } })
        every { getThemeMode() } returns flowOf(ThemeMode.SYSTEM)
        every { getUseDynamicColor() } returns flowOf(false)
        viewModel = SettingsViewModel(getThemeMode, setThemeMode, getUseDynamicColor, setUseDynamicColor)
    }

    @AfterTest
    fun tearDown() {
        viewModel.onCleared()
        stopTestKoin()
    }

    @Test
    fun `initial state has SYSTEM theme and dynamic color disabled`() = runTest {
        viewModel.state.value shouldBe SettingsState()
    }

    @Test
    fun `SetThemeMode action delegates to use case`() = runTest {
        everySuspend { setThemeMode(ThemeMode.DARK) } returns Unit

        viewModel.onAction(SettingsAction.SetThemeMode(ThemeMode.DARK))
        advanceUntilIdle()

        verifySuspend { setThemeMode(ThemeMode.DARK) }
    }

    @Test
    fun `SetUseDynamicColor action delegates to use case`() = runTest {
        everySuspend { setUseDynamicColor(true) } returns Unit

        viewModel.onAction(SettingsAction.SetUseDynamicColor(true))
        advanceUntilIdle()

        verifySuspend { setUseDynamicColor(true) }
    }
}
