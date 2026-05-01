package com.yourcompany.kmptemplate.core.domain.extensions

import com.yourcompany.kmptemplate.core.domain.AppError
import com.yourcompany.kmptemplate.core.domain.AppResult
import kotlin.reflect.KClass

class AppResultScope<T>(private val result: AppResult<T>) {
    private var successBlock: ((T) -> Unit)? = null
    private val failureBlocks = mutableListOf<(AppError) -> Boolean>()
    private var catchBlock: ((AppError) -> Unit)? = null

    fun success(block: (T) -> Unit) {
        successBlock = block
    }

    inline fun <reified E : AppError> failure(noinline block: (E) -> Unit) {
        failureBlocks += { error -> (error is E).also { if (it) block(error) } }
    }

    fun catch(block: (AppError) -> Unit) {
        catchBlock = block
    }

    fun execute() {
        when (result) {
            is AppResult.Success -> successBlock?.invoke(result.data)
            is AppResult.Failure -> {
                val handled = failureBlocks.any { it(result.error) }
                if (!handled) catchBlock?.invoke(result.error)
            }
        }
    }
}

fun <T> AppResult<T>.handle(block: AppResultScope<T>.() -> Unit) {
    AppResultScope(this).apply(block).execute()
}
