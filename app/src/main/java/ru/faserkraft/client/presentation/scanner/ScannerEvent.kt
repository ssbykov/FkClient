package ru.faserkraft.client.presentation.scanner

sealed interface ScannerEvent {
    data class OpenProduct(val serialNumber: String) : ScannerEvent
    data class OpenPackaging(val serialNumber: String) : ScannerEvent
    data class OpenDeviceRegistration(val qrContent: String) : ScannerEvent
    data class ShowError(val message: String) : ScannerEvent
}