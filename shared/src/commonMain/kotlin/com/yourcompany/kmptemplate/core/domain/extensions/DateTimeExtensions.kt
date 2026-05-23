package com.yourcompany.kmptemplate.core.domain.extensions

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

fun Instant.toLocalDate(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
    toLocalDateTime(timeZone).date

fun Instant.toFormattedDate(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val local = toLocalDateTime(timeZone)
    return "${local.year}-${local.monthNumber.toString().padStart(
        2,
        '0',
    )}-${local.dayOfMonth.toString().padStart(2, '0')}"
}
