package ru.faserkraft.client.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@RequiresApi(Build.VERSION_CODES.O)
private val uiFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        .withZone(ZoneId.systemDefault())

@RequiresApi(Build.VERSION_CODES.O)
fun formatIsoToUi(iso: String?): String {
    if (iso.isNullOrBlank()) return ""

    return try {
        val instant = Instant.parse(iso)
        uiFormatter.format(instant)
    } catch (e: Exception) {
        iso
    }
}


fun nowIsoUtc(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date())
}