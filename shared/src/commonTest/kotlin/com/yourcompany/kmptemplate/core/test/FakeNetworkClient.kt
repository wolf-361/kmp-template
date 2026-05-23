package com.yourcompany.kmptemplate.core.test

import com.yourcompany.kmptemplate.core.data.network.NetworkClient
import com.yourcompany.kmptemplate.core.domain.AppResult
import com.yourcompany.kmptemplate.core.domain.CoreError
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.util.reflect.TypeInfo

class FakeNetworkClient : NetworkClient() {

    private val queue = ArrayDeque<AppResult<*>>()

    @PublishedApi
    override val httpClient: HttpClient
        get() = error("FakeNetworkClient: httpClient is not available")

    fun enqueue(vararg results: AppResult<*>) {
        queue.addAll(results.toList())
    }

    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    override suspend fun <T> coreRequest(
        typeInfo: TypeInfo,
        block: HttpRequestBuilder.() -> Unit,
        shouldTriggerLogout: Boolean,
    ): AppResult<T> = (
        queue.removeFirstOrNull()
            ?: AppResult.Failure(CoreError.Network.NoConnection)
        ) as AppResult<T>
}
