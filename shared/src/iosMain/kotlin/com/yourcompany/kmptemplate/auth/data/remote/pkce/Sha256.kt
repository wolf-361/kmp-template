package com.yourcompany.kmptemplate.auth.data.remote.pkce

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH

@OptIn(ExperimentalForeignApi::class)
actual fun sha256(input: ByteArray): ByteArray = memScoped {
    val digest = allocArray<UByteVar>(CC_SHA256_DIGEST_LENGTH)
    CC_SHA256(input.refTo(0), input.size.toUInt(), digest)
    ByteArray(CC_SHA256_DIGEST_LENGTH) { digest[it].toByte() }
}
