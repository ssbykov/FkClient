package ru.faserkraft.client.presentation.registration

import QrGenerationEvent
import QrGenerationUiState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.faserkraft.client.api.Api
import ru.faserkraft.client.data.callApi
import ru.faserkraft.client.domain.usecase.employee.GetEmployeesUseCase
import ru.faserkraft.client.dto.toQrContent
import ru.faserkraft.client.presentation.base.toErrorMessage
import ru.faserkraft.client.utils.QrCodeGenerator
import javax.inject.Inject

@HiltViewModel
class QrGenerationViewModel @Inject constructor(
    private val getEmployeesUseCase: GetEmployeesUseCase,
    private val api: Api,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrGenerationUiState())
    val uiState: StateFlow<QrGenerationUiState> = _uiState

    private val _events = MutableSharedFlow<QrGenerationEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<QrGenerationEvent> = _events

    fun loadEmployees() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { getEmployeesUseCase() }
                .onSuccess { _uiState.update { state -> state.copy(employees = it) } }
                .onFailure { _events.emit(QrGenerationEvent.ShowError(it.toErrorMessage())) }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun generateQr(employeeId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching {
                val response = callApi { api.getQrCode(employeeId) }
                    ?: error("Пустой ответ от сервера")
                QrCodeGenerator.generate(response.toQrContent())
            }
                .onSuccess { _uiState.update { state -> state.copy(qrBitmap = it) } }
                .onFailure { _events.emit(QrGenerationEvent.ShowError(it.toErrorMessage())) }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }
}