package ru.faserkraft.client.ui.packaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.faserkraft.client.domain.model.FinishedProduct
import ru.faserkraft.client.domain.model.Packaging
import ru.faserkraft.client.domain.model.UiState
import ru.faserkraft.client.domain.usecase.packaging.CreatePackagingUseCase
import ru.faserkraft.client.domain.usecase.packaging.GetPackagingInStorageUseCase
import ru.faserkraft.client.domain.usecase.packaging.GetPackagingUseCase
import ru.faserkraft.client.domain.usecase.product.GetFinishedProductsUseCase
import javax.inject.Inject

/**
 * ViewModel для работы с упаковкой - обновлено для новой архитектуры
 */
@HiltViewModel
class PackagingViewModel @Inject constructor(
    private val getPackagingUseCase: GetPackagingUseCase,
    private val createPackagingUseCase: CreatePackagingUseCase,
    private val getPackagingInStorageUseCase: GetPackagingInStorageUseCase,
    private val getFinishedProductsUseCase: GetFinishedProductsUseCase
) : ViewModel() {

    private val _packagingState = MutableStateFlow<UiState<Packaging>>(UiState.Idle)
    val packagingState: StateFlow<UiState<Packaging>> = _packagingState

    private val _packagedInStorageState = MutableStateFlow<UiState<List<Packaging>>>(UiState.Idle)
    val packagingInStorageState: StateFlow<UiState<List<Packaging>>> = _packagedInStorageState

    private val _finishedProductsState = MutableStateFlow<UiState<List<FinishedProduct>>>(UiState.Idle)
    val finishedProductsState: StateFlow<UiState<List<FinishedProduct>>> = _finishedProductsState

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState

    /**
     * Получить упаковку по серийному номеру
     */
    fun getPackaging(serialNumber: String) {
        viewModelScope.launch {
            _packagingState.value = UiState.Loading
            getPackagingUseCase(serialNumber)
                .onSuccess { packaging ->
                    _packagingState.value = UiState.Success(packaging)
                }
                .onFailure { exception ->
                    _packagingState.value = UiState.Error(exception)
                }
        }
    }

    /**
     * Создать новую упаковку
     */
    fun createPackaging(serialNumber: String, productIds: List<Long>) {
        viewModelScope.launch {
            _actionState.value = ActionState.InProgress
            createPackagingUseCase(serialNumber, productIds)
                .onSuccess { packaging ->
                    _packagingState.value = UiState.Success(packaging)
                    _actionState.value = ActionState.Success("Упаковка создана")
                    getPackagingInStorage()
                }
                .onFailure { exception ->
                    _actionState.value = ActionState.Error(exception)
                }
        }
    }

    /**
     * Получить упаковку на хранении
     */
    fun getPackagingInStorage() {
        viewModelScope.launch {
            _packagedInStorageState.value = UiState.Loading
            getPackagingInStorageUseCase()
                .onSuccess { packagingList ->
                    _packagedInStorageState.value = UiState.Success(packagingList)
                }
                .onFailure { exception ->
                    _packagedInStorageState.value = UiState.Error(exception)
                }
        }
    }

    /**
     * Получить готовые товары для упаковки
     */
    fun getFinishedProducts() {
        viewModelScope.launch {
            _finishedProductsState.value = UiState.Loading
            getFinishedProductsUseCase()
                .onSuccess { products ->
                    _finishedProductsState.value = UiState.Success(products)
                }
                .onFailure { exception ->
                    _finishedProductsState.value = UiState.Error(exception)
                }
        }
    }

    /**
     * Сбросить состояние
     */
    fun clearPackaging() {
        _packagingState.value = UiState.Idle
        _actionState.value = ActionState.Idle
    }

    fun resetActionState() {
        _actionState.value = ActionState.Idle
    }

    sealed class ActionState {
        object Idle : ActionState()
        object InProgress : ActionState()
        data class Success(val message: String) : ActionState()
        data class Error(val exception: Throwable) : ActionState()
    }
}


