package ru.faserkraft.client.utils

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

fun formatIsoToUi(iso: String): String {
    val parsed = OffsetDateTime.parse(iso) // "2025-11-05T17:31:30.621017Z"
    val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
    return parsed.format(formatter)       // "05-11-2025 17:31"
}