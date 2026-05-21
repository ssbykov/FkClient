package ru.faserkraft.client.utils.timeprovider

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class RealTimeProvider @Inject constructor() : TimeProvider {
    override fun nowIsoUtc(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
    override fun nowLocalDate(): LocalDate {
        return LocalDate.now() // <-- Реализация нового метода
    }
}