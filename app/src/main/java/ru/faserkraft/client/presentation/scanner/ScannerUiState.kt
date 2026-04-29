package ru.faserkraft.client.presentation.scanner

data class ScannerUiState(
    val isLoading: Boolean = false,
    val lastScannedValue: String? = null,
)