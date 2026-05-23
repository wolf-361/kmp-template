package com.yourcompany.kmptemplate.core.data.network

import com.yourcompany.kmptemplate.core.domain.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.util.reflect.TypeInfo
import io.ktor.util.reflect.typeInfo

expect fun createHttpEngine(): HttpClientEngine

abstract class NetworkClient {

    @PublishedApi
    internal abstract val httpClient: HttpClient

    @PublishedApi
    internal abstract suspend fun <T> coreRequest(
        typeInfo: TypeInfo,
        block: HttpRequestBuilder.() -> Unit,
        shouldTriggerLogout: Boolean,
    ): AppResult<T>

    suspend inline fun <reified T> request(noinline block: HttpRequestBuilder.() -> Unit): AppResult<T> =
        coreRequest(typeInfo<T>(), block, shouldTriggerLogout = true)

    suspend inline fun <reified T> unauthorizedRequest(noinline block: HttpRequestBuilder.() -> Unit): AppResult<T> =
        coreRequest(typeInfo<T>(), block, shouldTriggerLogout = false)
}
