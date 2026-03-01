package ru.faserkraft.client.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.text.format


@SuppressLint("ConstantLocale")
val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
@SuppressLint("ConstantLocale")
val uiFormat  = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

val apiPattern = Regex("""\d{4}-\d{2}-\d{2}""")   // yyyy-MM-dd
val uiPattern  = Regex("""\d{2}\.\d{2}\.\d{4}""") // dd.MM.yyyy

fun convertDate(dateStr: String): String {
    return when {
        apiPattern.matches(dateStr) -> {
            uiFormat.format(apiFormat.parse(dateStr)!!)
        }
        uiPattern.matches(dateStr) -> {
            apiFormat.format(uiFormat.parse(dateStr)!!)
        }
        else -> dateStr
    }
}

fun isPlanDateEditable(planDateApi: String): Boolean {
    val planDate = apiFormat.parse(planDateApi) ?: return false
    val today = apiFormat.parse(apiFormat.format(Date()))!!
    // true, если дата плана сегодня или в будущем
    return !planDate.before(today)
}

fun formatPlanDate(timeMillis: Long): Pair<String, String> {
    val date = Date(timeMillis)
    val apiDate = apiFormat.format(date)
    val uiDate = uiFormat.format(date)
    return apiDate to uiDate
}

fun converterFromMillis(dateMillis: Long, pattern: String): String{
    val dateFormat = SimpleDateFormat(pattern, Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Europe/Moscow")
    }
    return dateFormat.format(dateMillis)
}