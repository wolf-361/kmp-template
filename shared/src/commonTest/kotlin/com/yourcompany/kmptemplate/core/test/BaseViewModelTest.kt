package com.yourcompany.kmptemplate.core.test

import app.cash.turbine.test
import com.yourcompany.kmptemplate.core.presentation.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseViewModelTest {

    protected val testDispatcher = StandardTestDispatcher()
    protected val testScope = TestScope(testDispatcher)

    @BeforeTest
    fun setUpDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDownDispatcher() {
        Dispatchers.resetMain()
    }

    protected fun runTest(block: suspend TestScope.() -> Unit) = testScope.runTest(testBody = block)

    protected suspend fun <S, A, E> BaseViewModel<S, A, E>.awaitState(block: suspend (S) -> Unit) =
        state.test { block(awaitItem()) }

    protected suspend fun <S, A, E> BaseViewModel<S, A, E>.awaitEffect(block: suspend (E) -> Unit) =
        effects.test { block(awaitItem()) }
}
