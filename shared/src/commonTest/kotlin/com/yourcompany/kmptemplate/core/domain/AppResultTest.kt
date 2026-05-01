package com.yourcompany.kmptemplate.core.domain

import com.yourcompany.kmptemplate.core.domain.extensions.handle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AppResultTest {

    @Test
    fun `Success map transforms data`() {
        val result = AppResult.Success(42).map { it * 2 }
        assertIs<AppResult.Success<Int>>(result)
        assertEquals(84, result.data)
    }

    @Test
    fun `Failure map is a no-op`() {
        val error = CoreError.Unauthorized
        val result: AppResult<Int> = AppResult.Failure(error)
        val mapped = result.map { it * 2 }
        assertIs<AppResult.Failure>(mapped)
        assertEquals(error, mapped.error)
    }

    @Test
    fun `flatMap chains successful results`() {
        val result = AppResult.Success(10)
            .flatMap { AppResult.Success(it + 5) }
        assertIs<AppResult.Success<Int>>(result)
        assertEquals(15, result.data)
    }

    @Test
    fun `flatMap short-circuits on Failure`() {
        val result: AppResult<Int> = AppResult.Failure(CoreError.Network.Timeout)
            .flatMap { AppResult.Success(42) }
        assertIs<AppResult.Failure>(result)
    }

    @Test
    fun `toUnit wraps data in Unit`() {
        val result = AppResult.Success("hello").toUnit()
        assertIs<AppResult.Success<Unit>>(result)
        assertEquals(Unit, result.data)
    }

    @Test
    fun `handle success block is called`() {
        var captured: String? = null
        AppResult.Success("hello").handle {
            success { captured = it }
        }
        assertEquals("hello", captured)
    }

    @Test
    fun `handle typed failure block is called for matching error`() {
        var called = false
        AppResult.Failure(CoreError.Network.Timeout).handle {
            failure<CoreError.Network.Timeout> { called = true }
        }
        assertTrue(called)
    }

    @Test
    fun `handle catch block fires when no typed failure matches`() {
        var caught: AppError? = null
        AppResult.Failure(CoreError.DataCorruption).handle {
            failure<CoreError.Unauthorized> { /* won't fire */ }
            catch { caught = it }
        }
        assertEquals(CoreError.DataCorruption, caught)
    }

    @Test
    fun `handle success block not called on Failure`() {
        var called = false
        AppResult.Failure(CoreError.Unauthorized).handle {
            success { called = true }
        }
        assertFalse(called)
    }
}
