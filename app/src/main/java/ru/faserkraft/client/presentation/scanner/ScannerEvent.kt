package ru.faserkraft.client.presentation.scanner

import ru.faserkraft.client.domain.model.DeviceRequest
import ru.faserkraft.client.domain.qr.QrInputSource

sealed interface ScannerEvent {
    data class OpenProduct(val code: String, val source: QrInputSource) : ScannerEvent
    data class OpenPackaging(val code: String) : ScannerEvent
    data class OpenDeviceRegistration(val request: DeviceRequest) : ScannerEvent
    data class ShowError(val message: String) : ScannerEvent
}
