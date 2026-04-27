package ru.faserkraft.client.ui.qr

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.faserkraft.client.domain.model.ActionState
import ru.faserkraft.client.utils.QrCodeGenerator
import javax.inject.Inject

/**
 * ViewModel для работы с QR кодами
 */
@HiltViewModel
class QrViewModel @Inject constructor() : ViewModel() {

    private val _qrBitmapState = MutableStateFlow<Bitmap?>(null)
    val qrBitmapState: StateFlow<Bitmap?> = _qrBitmapState

    private val _generationState = MutableStateFlow<ActionState>(ActionState.Idle)
    val generationState: StateFlow<ActionState> = _generationState

    private val _qrScannedEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val qrScannedEvents: SharedFlow<String> = _qrScannedEvents

    /**
     * Сгенерировать QR код
     */
    fun generateQrCode(content: String) {
        viewModelScope.launch {
            try {
                _generationState.value = ActionState.InProgress
                val bitmap = QrCodeGenerator.generate(content)
                _qrBitmapState.value = bitmap
                _generationState.value = ActionState.Success("QR код сгенерирован")
            } catch (e: Exception) {
                _generationState.value = ActionState.Error(e)
            }
        }
    }

    /**
     * Обработать отсканированный QR код
     */
    fun onQrScanned(content: String) {
        _qrScannedEvents.tryEmit(content)
    }

    /**
     * Очистить QR код
     */
    fun clearQrCode() {
        _qrBitmapState.value = null
        _generationState.value = ActionState.Idle
    }
}

