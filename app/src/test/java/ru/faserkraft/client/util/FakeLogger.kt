package ru.faserkraft.client.util

import ru.faserkraft.client.utils.Logger

class FakeLogger : Logger {

    data class LogEntry(
        val tag: String,
        val message: String,
        val throwable: Throwable? = null
    )

    val debugs = mutableListOf<LogEntry>()
    val infos = mutableListOf<LogEntry>()
    val warnings = mutableListOf<LogEntry>()
    val errors = mutableListOf<LogEntry>()

    override fun d(tag: String, message: String) {
        debugs += LogEntry(tag, message)
    }

    override fun i(tag: String, message: String) {
        infos += LogEntry(tag, message)
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        warnings += LogEntry(tag, message, throwable)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        errors += LogEntry(tag, message, throwable)
    }
}