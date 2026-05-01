package com.yourcompany.kmptemplate.core.domain

sealed interface CoreError : AppError {
    sealed interface Network : CoreError {
        data object Timeout : Network
        data object NoConnection : Network
        data class BackendPayload(val code: Int, val message: String) : Network
    }
    data object DataCorruption : CoreError
    data object Database : CoreError
    data object Unauthorized : CoreError
}
