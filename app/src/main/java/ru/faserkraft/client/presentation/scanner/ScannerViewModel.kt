package ru.faserkraft.client.presentation.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.faserkraft.client.utils.isUfCode
import ru.faserkraft.client.utils.isUfPkgCode
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState

    private val _events = MutableSharedFlow<ScannerEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ScannerEvent> = _events

    private var isHandled = false

    fun resetHandled() {
        isHandled = false
    }

    fun clearState() {
        isHandled = false
        _uiState.update { ScannerUiState() }
    }

    fun decodeQrCode(raw: String) {
        if (isHandled) return
        isHandled = true

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, lastScannedValue = raw) }
            when {
                isUfCode(raw) -> _events.emit(ScannerEvent.OpenProduct(raw))
                isUfPkgCode(raw) -> _events.emit(ScannerEvent.OpenPackaging(raw))
                else -> _events.emit(ScannerEvent.OpenDeviceRegistration(raw))
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}