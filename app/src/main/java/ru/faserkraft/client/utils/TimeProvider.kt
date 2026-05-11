package ru.faserkraft.client.utils


interface TimeProvider {
    fun nowIsoUtc(): String
}