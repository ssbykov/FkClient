package ru.faserkraft.client.domain.qr

import ru.faserkraft.client.domain.model.DeviceRequest

sealed interface QrParseResult {
    data class Product(val code: String) : QrParseResult
    data class Packaging(val code: String) : QrParseResult
    data class DeviceRegistration(val request: DeviceRequest) : QrParseResult
    data object Unknown : QrParseResult
}