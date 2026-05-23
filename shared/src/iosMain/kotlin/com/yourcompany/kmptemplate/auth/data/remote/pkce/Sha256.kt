package com.yourcompany.kmptemplate.auth.data.remote.pkce

import okio.ByteString.Companion.toByteString

actual fun sha256(input: ByteArray): ByteArray = input.toByteString().sha256().toByteArray()
