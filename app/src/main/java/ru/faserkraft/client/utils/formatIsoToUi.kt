package ru.faserkraft.client.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun formatIsoToUi(iso: String): String {
    // разбор ISO вида 2025-11-05T17:31:30.621017Z
    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val date = isoFormat.parse(iso) ?: return ""

    // формат для UI
    val uiFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
    return uiFormat.format(date)
}

fun nowIsoUtc(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date())
}