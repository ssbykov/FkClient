package ru.faserkraft.client.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.Step
import ru.faserkraft.client.domain.model.UiState
import ru.faserkraft.client.domain.usecase.product.CompleteStepUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductUseCase
import ru.faserkraft.client.domain.usecase.product.UpdateProductStatusUseCase
import javax.inject.Inject

/**
 * ViewModel для работы с товарами
 * Обновлено для поддержки новой архитектуры с Use Cases
 */
@HiltViewModel
class ProductViewModel @Inject constructor(
    private val getProductUseCase: GetProductUseCase,
    private val completeStepUseCase: CompleteStepUseCase,
    private val updateProductStatusUseCase: UpdateProductStatusUseCase
) : ViewModel() {

    private val _productState = MutableStateFlow<UiState<Product>>(UiState.Idle)
    val productState: StateFlow<UiState<Product>> = _productState

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState

    // Вычисляемое состояние последнего шага на основе товара
    val lastStepState: StateFlow<UiState<Step>> = _productState
        .map { productState ->
            when (productState) {
                is UiState.Success -> {
                    val lastStep = productState.data.steps.maxByOrNull { it.stepDefinition.order }
                    if (lastStep != null) {
                        UiState.Success(lastStep)
                    } else {
                        UiState.Error(IllegalStateException("No steps found"))
                    }
                }
                is UiState.Loading -> UiState.Loading
                is UiState.Error -> UiState.Error(productState.exception)
                is UiState.Idle -> UiState.Idle
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Idle)

    /**
     * Получить товар по серийному номеру
     */
    fun getProduct(serialNumber: String) {
        viewModelScope.launch {
            _productState.value = UiState.Loading
            getProductUseCase(serialNumber)
                .onSuccess { product ->
                    _productState.value = UiState.Success(product)
                }
                .onFailure { exception ->
                    _productState.value = UiState.Error(exception)
                }
        }
    }

    /**
     * Завершить текущий шаг товара
     */
    fun completeCurrentStep() {
        val currentProduct = (_productState.value as? UiState.Success)?.data ?: return

        val currentStep = currentProduct.steps.firstOrNull { it.status != "done" }
        val stepId = currentStep?.id ?: return

        viewModelScope.launch {
            _actionState.value = ActionState.InProgress
            completeStepUseCase(stepId)
                .onSuccess { updatedProduct ->
                    _productState.value = UiState.Success(updatedProduct)
                    _actionState.value = ActionState.Success("Шаг завершен")
                }
                .onFailure { exception ->
                    _actionState.value = ActionState.Error(exception)
                }
        }
    }

    /**
     * Обновить статус товара
     */
    fun updateProductStatus(status: String) {
        val currentProduct = (_productState.value as? UiState.Success)?.data ?: return

        viewModelScope.launch {
            _actionState.value = ActionState.InProgress
            updateProductStatusUseCase(currentProduct.id, status)
                .onSuccess { updatedProduct ->
                    _productState.value = UiState.Success(updatedProduct)
                    _actionState.value = ActionState.Success("Статус обновлен")
                }
                .onFailure { exception ->
                    _actionState.value = ActionState.Error(exception)
                }
        }
    }

    /**
     * Сбросить состояние товара
     */
    fun clearProduct() {
        _productState.value = UiState.Idle
        _actionState.value = ActionState.Idle
    }

    /**
     * Сбросить состояние действия
     */
    fun resetActionState() {
        _actionState.value = ActionState.Idle
    }

    /**
     * Состояния действий (завершение шага, обновление статуса)
     */
    sealed class ActionState {
        object Idle : ActionState()
        object InProgress : ActionState()
        data class Success(val message: String) : ActionState()
        data class Error(val exception: Throwable) : ActionState()
    }
}
