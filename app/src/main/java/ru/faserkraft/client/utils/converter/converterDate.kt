package ru.faserkraft.client.utils.converter

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Формат даты, используемый в API бэкенда (yyyy-MM-dd) */
@SuppressLint("ConstantLocale")
val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

/** Формат даты, отображаемый пользователю в интерфейсе (dd.MM.yyyy) */
@SuppressLint("ConstantLocale")
val uiFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

/** Формат полной даты и времени со временем (ISO / дата упаковки) */
@SuppressLint("ConstantLocale")
val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

/** Формат отображения даты и времени в карточке упаковки (дд.мм.гггг, чч:мм) */
@SuppressLint("ConstantLocale")
val packagingDateTimeUiFormat = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault())

/** Регулярное выражение для проверки соответствия формату API (гггг-мм-дд) */
val apiPattern = Regex("""\d{4}-\d{2}-\d{2}""")

/** Регулярное выражение для проверки соответствия формату UI (дд.мм.гггг) */
val uiPattern = Regex("""\d{2}\.\d{2}\.\d{4}""")

/**
 * Универсальный конвертер даты:
 * - Если на входе строка "yyyy-MM-dd" (из API) -> преобразует в "dd.MM.yyyy" (для UI).
 * - Если на входе строка "dd.MM.yyyy" (из UI) -> преобразует в "yyyy-MM-dd" (для API).
 * - В остальных случаях возвращает исходную строку без изменений.
 */
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

/**
 * Конвертирует строку даты/времени ISO или обычную дату в удобный для карточки упаковки вид ("dd.MM.yyyy, HH:mm" или "dd.MM.yyyy").
 * Безопасен к ошибкам парсинга (в случае несовпадения вернет исходную строку).
 */
fun formatPackagingDate(dateStr: String): String {
    return runCatching {
        // Пробуем распарсить как полный ISO с временем: "2026-09-20T11:25:00"
        val cleanIso = if (dateStr.contains(".")) dateStr.substringBefore(".") else dateStr
        val parsedIso = isoFormat.parse(cleanIso)
        if (parsedIso != null) {
            return@runCatching packagingDateTimeUiFormat.format(parsedIso)
        }

        // Если это простая дата "yyyy-MM-dd"
        if (apiPattern.matches(dateStr)) {
            val parsedApi = apiFormat.parse(dateStr)
            if (parsedApi != null) return@runCatching uiFormat.format(parsedApi)
        }

        dateStr
    }.getOrDefault(convertDate(dateStr))
}

/**
 * Проверяет, можно ли редактировать плановую дату отгрузки.
 * Редактирование разрешено, если плановая дата — сегодня или в будущем.
 *
 * @param planDateApi Дата в формате API ("yyyy-MM-dd").
 * @return `true`, если дата сегодня или позже, иначе `false`.
 */
fun isPlanDateEditable(planDateApi: String): Boolean {
    val planDate = apiFormat.parse(planDateApi) ?: return false
    val today = apiFormat.parse(apiFormat.format(Date()))!!
    // true, если дата плана сегодня или в будущем
    return !planDate.before(today)
}

/**
 * Форматирует миллисекунды (например, полученные из MaterialDatePicker)
 * в пару значений: First = формат API ("yyyy-MM-dd"), Second = формат UI ("dd.MM.yyyy").
 */
fun formatPlanDate(timeMillis: Long): Pair<String, String> {
    val date = Date(timeMillis)
    val apiDate = apiFormat.format(date)
    val uiDate = uiFormat.format(date)
    return apiDate to uiDate
}

/**
 * Возвращает сегодняшнюю дату в формате API ("yyyy-MM-dd").
 */
fun getToday(): String {
    val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return apiFormat.format(Date())
}
