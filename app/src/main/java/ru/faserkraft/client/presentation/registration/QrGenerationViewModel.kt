package ru.faserkraft.client.presentation.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.faserkraft.client.domain.usecase.employee.GetEmployeeQrContentUseCase
import ru.faserkraft.client.domain.usecase.employee.GetEmployeesUseCase
import ru.faserkraft.client.presentation.base.toErrorMessage
import ru.faserkraft.client.utils.QrCodeGeneratorWrapper
import javax.inject.Inject

@HiltViewModel
class QrGenerationViewModel @Inject constructor(
    private val getEmployeesUseCase: GetEmployeesUseCase,
    private val getEmployeeQrContentUseCase: GetEmployeeQrContentUseCase,
    private val qrCodeGenerator: QrCodeGeneratorWrapper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrGenerationUiState())
    val uiState: StateFlow<QrGenerationUiState> = _uiState

    // Используем Channel для гарантированной доставки разовых событий (ошибок, навигации)
    private val _events = Channel<QrGenerationEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun loadEmployees() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            runCatching { getEmployeesUseCase() }
                .onSuccess { employeesList ->
                    _uiState.update { state -> state.copy(employees = employeesList) }
                }
                .onFailure { error ->
                    _events.send(QrGenerationEvent.ShowError(error.toErrorMessage()))
                }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun generateQr(employeeId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching {
                val qrContent = getEmployeeQrContentUseCase(employeeId)
                withContext(Dispatchers.Default) {
                    qrCodeGenerator.generate(qrContent)
                }
            }
                .onSuccess { bitmap ->
                    _uiState.update { state -> state.copy(qrBitmap = bitmap) }
                }
                .onFailure { error ->
                    _events.send(QrGenerationEvent.ShowError(error.toErrorMessage()))
                }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }
}