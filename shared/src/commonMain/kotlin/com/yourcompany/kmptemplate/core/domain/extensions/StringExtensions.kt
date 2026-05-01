package com.yourcompany.kmptemplate.core.domain.extensions

fun String.trimAndLowercase(): String = trim().lowercase()

fun String.isValidEmail(): Boolean =
    isNotBlank() && contains('@') && contains('.')

fun String.ellipsize(maxLength: Int): String =
    if (length <= maxLength) this else "${take(maxLength - 1)}…"
