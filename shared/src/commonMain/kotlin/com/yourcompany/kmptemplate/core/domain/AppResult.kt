package com.yourcompany.kmptemplate.core.domain

sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()

    sealed class Error : AppResult<Nothing>() {
        data class Network(val throwable: Throwable) : Error()
        data class Unexpected(val throwable: Throwable) : Error()
    }
}

inline fun <T> AppResult<T>.onSuccess(block: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) block(data)
    return this
}

inline fun <T> AppResult<T>.onError(block: (AppResult.Error) -> Unit): AppResult<T> {
    if (this is AppResult.Error) block(this)
    return this
}
