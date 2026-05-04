package ru.faserkraft.client.presentation.scanner

import ru.faserkraft.client.dto.DeviceRequestDto

sealed interface ScannerEvent {
    data class OpenProduct(val code: String) : ScannerEvent
    data class OpenPackaging(val code: String) : ScannerEvent
    data class OpenDeviceRegistration(val request: DeviceRequestDto) : ScannerEvent
    data class ShowError(val message: String) : ScannerEvent
}