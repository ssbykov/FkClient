package ru.faserkraft.client.utils.converter

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val isoParser: SimpleDateFormat
    get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

private val uiFormatter: SimpleDateFormat
    get() = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }

fun formatIsoToUi(iso: String?): String {
    if (iso.isNullOrBlank()) return "-"

    return try {
        val date = isoParser.parse(iso)

        if (date != null) {
            uiFormatter.format(date)
        } else {
            iso
        }
    } catch (e: Exception) {
        iso ?: "-"
    }
}
