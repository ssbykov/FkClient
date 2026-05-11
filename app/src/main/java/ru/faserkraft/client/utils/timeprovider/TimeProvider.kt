package ru.faserkraft.client.utils.timeprovider

interface TimeProvider {
    fun nowIsoUtc(): String
}