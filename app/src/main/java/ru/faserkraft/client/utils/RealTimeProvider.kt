package ru.faserkraft.client.utils


class RealTimeProvider : TimeProvider {
    override fun nowIsoUtc(): String = nowIsoUtc()
}