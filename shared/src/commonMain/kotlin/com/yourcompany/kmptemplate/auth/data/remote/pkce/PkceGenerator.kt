package com.yourcompany.kmptemplate.auth.data.remote.pkce

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

object PkceGenerator {
    fun generateVerifier(): String = Random.nextBytes(32).encodeBase64Url()

    fun generateChallenge(verifier: String): String =
        sha256(verifier.encodeToByteArray()).encodeBase64Url()
}

expect fun sha256(input: ByteArray): ByteArray

@OptIn(ExperimentalEncodingApi::class)
private fun ByteArray.encodeBase64Url(): String =
    Base64.UrlSafe.encode(this).trimEnd('=')
