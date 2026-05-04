package ru.faserkraft.client.domain.qr

import ru.faserkraft.client.dto.DeviceRequestDto

sealed interface QrParseResult {
    data class Product(val code: String) : QrParseResult
    data class Packaging(val code: String) : QrParseResult
    data class DeviceRegistration(val request: DeviceRequestDto) : QrParseResult
    data object Unknown : QrParseResult
}