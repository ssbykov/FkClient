package ru.faserkraft.client.presentation.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.faserkraft.client.domain.qr.QrClassifier
import ru.faserkraft.client.domain.qr.QrInputSource
import ru.faserkraft.client.domain.qr.QrParseResult
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val qrClassifier: QrClassifier,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState

    private val _events = Channel<ScannerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var isHandled = false

    fun resetHandled() {
        isHandled = false
    }

    fun clearState() {
        isHandled = false
        _uiState.update { ScannerUiState() }
    }

    fun decodeQrCode(raw: String, source: QrInputSource) {
        if (isHandled) return
        isHandled = true

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, lastScannedValue = raw) }

            try {
                when (val result = qrClassifier.classify(raw)) {
                    is QrParseResult.Product ->
                        _events.send(ScannerEvent.OpenProduct(result.code, source))

                    is QrParseResult.Packaging ->
                        _events.send(ScannerEvent.OpenPackaging(result.code))

                    is QrParseResult.DeviceRegistration -> {
                        if (source == QrInputSource.CAMERA) {
                            _events.send(ScannerEvent.OpenDeviceRegistration(result.request))
                        } else {
                            isHandled = false
                            _events.send(
                                ScannerEvent.ShowError(
                                    "Регистрация устройства доступна только при сканировании QR-кода"
                                )
                            )
                        }
                    }

                    QrParseResult.Unknown -> {
                        isHandled = false
                        _events.send(ScannerEvent.ShowError("Нераспознанный QR-код"))
                    }
                }
            } catch (e: Exception) {
                isHandled = false
                _events.send(ScannerEvent.ShowError("Ошибка при чтении QR-кода"))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
