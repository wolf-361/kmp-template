package com.yourcompany.kmptemplate.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AppResultTest {

    @Test
    fun `success triggers onSuccess with data`() {
        val result: AppResult<String> = AppResult.Success("hello")
        var captured: String? = null
        result.onSuccess { captured = it }
        assertEquals("hello", captured)
    }

    @Test
    fun `success does not trigger onError`() {
        val result: AppResult<String> = AppResult.Success("hello")
        var called = false
        result.onError { called = true }
        assertFalse(called)
    }

    @Test
    fun `network error triggers onError as Network`() {
        val exception = RuntimeException("network failure")
        val result: AppResult<String> = AppResult.Error.Network(exception)
        var captured: AppResult.Error? = null
        result.onError { captured = it }
        val networkError = assertIs<AppResult.Error.Network>(captured)
        assertEquals(exception, networkError.throwable)
    }

    @Test
    fun `unexpected error triggers onError as Unexpected`() {
        val exception = RuntimeException("crash")
        val result: AppResult<String> = AppResult.Error.Unexpected(exception)
        var captured: AppResult.Error? = null
        result.onError { captured = it }
        val unexpectedError = assertIs<AppResult.Error.Unexpected>(captured)
        assertEquals(exception, unexpectedError.throwable)
    }

    @Test
    fun `error does not trigger onSuccess`() {
        val result: AppResult<String> = AppResult.Error.Network(RuntimeException())
        var called = false
        result.onSuccess { called = true }
        assertFalse(called)
    }

    @Test
    fun `onSuccess is chainable`() {
        val result: AppResult<Int> = AppResult.Success(42)
        var count = 0
        result.onSuccess { count++ }.onSuccess { count++ }
        assertEquals(2, count)
    }
}
