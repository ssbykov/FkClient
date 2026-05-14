package ru.faserkraft.client.utils.ext

/**
 * Сравнивает текущую строку версии с [currentVersion].
 * Поддерживает форматы разной длины (например, "1.2" и "1.2.1").
 * Возвращает true, если текущая версия новее.
 */
fun String.isNewerThan(currentVersion: String): Boolean {
    // Отбрасываем возможные суффиксы (-beta, -rc и т.д.)
    val latestClean = this.substringBefore("-").substringBefore("_")
    val currentClean = currentVersion.substringBefore("-").substringBefore("_")

    val latestParts = latestClean.split(".").map { it.toIntOrNull() ?: 0 }
    val currentParts = currentClean.split(".").map { it.toIntOrNull() ?: 0 }

    val maxLength = maxOf(latestParts.size, currentParts.size)
    for (i in 0 until maxLength) {
        val latestPart = latestParts.getOrElse(i) { 0 }
        val currentPart = currentParts.getOrElse(i) { 0 }

        if (latestPart > currentPart) return true
        if (latestPart < currentPart) return false
    }
    return false
}