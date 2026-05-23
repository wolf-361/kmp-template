package com.yourcompany.kmptemplate.core.test

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun buildTestHttpClient(handler: MockRequestHandler): HttpClient = HttpClient(MockEngine(handler)) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            },
        )
    }
}

fun jsonResponse(body: String, status: HttpStatusCode = HttpStatusCode.OK): MockRequestHandler = {
    respond(
        content = body,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )
}

fun errorResponse(status: HttpStatusCode = HttpStatusCode.InternalServerError): MockRequestHandler = {
    respond(content = "", status = status)
}
