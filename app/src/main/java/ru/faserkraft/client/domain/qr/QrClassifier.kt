package ru.faserkraft.client.domain.qr

interface QrClassifier {
    fun classify(raw: String): QrParseResult
}