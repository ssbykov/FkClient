package ru.faserkraft.client.utils.converter

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val uiFormatter: SimpleDateFormat
    get() = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }

private val baseIsoParser: SimpleDateFormat
    get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

fun formatIsoToUi(iso: String?): String {
    if (iso.isNullOrBlank()) return "-"

    return try {
        val cleanIso = iso.replace(Regex("\\.\\d+"), "")

        val date = baseIsoParser.parse(cleanIso)

        if (date != null) {
            uiFormatter.format(date)
        } else {
            iso
        }
    } catch (e: Exception) {
        iso ?: "-"
    }
}
