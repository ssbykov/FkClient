package ru.faserkraft.client.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun formatIsoToUi(iso: String): String {
    val parserWithMillis = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val parserNoMillis = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    val date = try {
        parserWithMillis.parse(iso)
    } catch (_: ParseException) {
        parserNoMillis.parse(iso)
    } ?: return ""

    val uiFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
    return uiFormat.format(date)
}


fun nowIsoUtc(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date())
}