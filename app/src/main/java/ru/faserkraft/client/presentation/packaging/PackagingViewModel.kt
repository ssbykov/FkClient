package ru.faserkraft.client.presentation.packaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.faserkraft.client.auth.AppAuth
import ru.faserkraft.client.domain.model.Packaging
import ru.faserkraft.client.domain.usecase.packaging.CreatePackagingUseCase
import ru.faserkraft.client.domain.usecase.packaging.DeletePackagingUseCase
import ru.faserkraft.client.domain.usecase.packaging.GetPackagingInStorageUseCase
import ru.faserkraft.client.domain.usecase.packaging.GetPackagingUseCase
import ru.faserkraft.client.domain.usecase.product.GetFinishedProductsUseCase
import ru.faserkraft.client.presentation.app.AppSessionCoordinator
import ru.faserkraft.client.presentation.app.AppSessionEvent
import ru.faserkraft.client.presentation.base.toErrorMessage
import javax.inject.Inject

@HiltViewModel
class PackagingViewModel @Inject constructor(
    private val getPackagingUseCase: GetPackagingUseCase,
    private val createPackagingUseCase: CreatePackagingUseCase,
    private val deletePackagingUseCase: DeletePackagingUseCase,
    private val getPackagingInStorageUseCase: GetPackagingInStorageUseCase,
    private val getFinishedProductsUseCase: GetFinishedProductsUseCase,
    private val appAuth: AppAuth,
    private val sessionCoordinator: AppSessionCoordinator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PackagingUiState())
    val uiState: StateFlow<PackagingUiState> = _uiState

    private val _events = MutableSharedFlow<PackagingEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<PackagingEvent> = _events

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(currentUser = appAuth.getRegistrationData())
            }
        }
        observeSessionEvents()
    }

    private fun observeSessionEvents() {
        viewModelScope.launch {
            sessionCoordinator.events.collect { event ->
                when (event) {
                    AppSessionEvent.Logout -> resetState()
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = PackagingUiState()
    }

    // ---------- Загрузка упаковки по серийному номеру (вход из QR) ----------

    fun loadPackaging(serialNumber: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { getPackagingUseCase(serialNumber) }
                .onSuccess { packaging ->
                    if (packaging == null) newPackaging(serialNumber)
                    else setPackaging(packaging)
                }
                .onFailure { e ->
                    if (e is retrofit2.HttpException && e.code() == 404) {
                        newPackaging(serialNumber)
                    } else {
                        emitError(e)
                    }
                }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun setPackaging(packaging: Packaging) {
        _uiState.update { it.copy(currentPackaging = packaging) }
        viewModelScope.launch { _events.emit(PackagingEvent.NavigateToPackaging) }
    }

    private fun newPackaging(serialNumber: String) {
        _uiState.update {
            it.copy(
                currentPackaging = Packaging(
                    id = 0,
                    serialNumber = serialNumber,
                    performedBy = null,
                    performedAt = null,
                    orderId = null,
                    products = emptyList(),
                )
            )
        }
        viewModelScope.launch {
            loadAvailableProducts()
            _events.emit(PackagingEvent.NavigateToNewPackaging)
        }
    }

    // ---------- Создание ----------

    fun createPackaging(serialNumber: String, productIds: List<Int>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching { createPackagingUseCase(serialNumber, productIds) }
                .onSuccess { setPackaging(it) }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    // ---------- Редактирование ----------

    fun onEditClicked() {
        viewModelScope.launch {
            _events.emit(PackagingEvent.NavigateToEdit)
        }
    }

    // ---------- Удаление ----------

    fun deletePackaging(serialNumber: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching { deletePackagingUseCase(serialNumber) }
                .onSuccess {
                    _uiState.update { state -> state.copy(currentPackaging = null) }
                    _events.emit(PackagingEvent.PackagingDeleted)
                }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    // ---------- Склад ----------

    fun loadPackagingInStorage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { getPackagingInStorageUseCase() }
                .onSuccess { _uiState.update { state -> state.copy(packagingInStorage = it) } }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // ---------- Продукты для упаковки ----------

    fun loadAvailableProducts() {
        viewModelScope.launch {
            runCatching { getFinishedProductsUseCase() }
                .onSuccess { _uiState.update { state -> state.copy(availableProducts = it) } }
                .onFailure { emitError(it) }
        }
    }

    // ---------- Вспомогательное ----------

    private suspend fun emitError(e: Throwable) {
        _events.emit(PackagingEvent.ShowError(e.toErrorMessage()))
    }

}