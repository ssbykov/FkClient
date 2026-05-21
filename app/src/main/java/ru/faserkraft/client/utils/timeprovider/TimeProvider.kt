package ru.faserkraft.client.utils.timeprovider

import java.time.LocalDate

interface TimeProvider {
    fun nowIsoUtc(): String
    fun nowLocalDate(): LocalDate
}