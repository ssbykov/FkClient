package ru.faserkraft.client.presentation.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.model.Step
import ru.faserkraft.client.domain.model.StepStatus
import ru.faserkraft.client.domain.usecase.employee.GetEmployeesUseCase
import ru.faserkraft.client.domain.usecase.process.GetProcessesUseCase
import ru.faserkraft.client.domain.usecase.product.ChangeProductProcessUseCase
import ru.faserkraft.client.domain.usecase.product.ChangeProductStatusUseCase
import ru.faserkraft.client.domain.usecase.product.CreateProductUseCase
import ru.faserkraft.client.domain.usecase.product.GetFinishedProductsUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductsByLastStepUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductsByStepEmployeeDayUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductsInventoryUseCase
import ru.faserkraft.client.domain.usecase.step.ChangeStepPerformerUseCase
import ru.faserkraft.client.domain.usecase.step.CloseStepUseCase
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val getProductUseCase: GetProductUseCase,
    private val createProductUseCase: CreateProductUseCase,
    private val changeProductStatusUseCase: ChangeProductStatusUseCase,
    private val changeProductProcessUseCase: ChangeProductProcessUseCase,
    private val closeStepUseCase: CloseStepUseCase,
    private val changeStepPerformerUseCase: ChangeStepPerformerUseCase,
    private val getProcessesUseCase: GetProcessesUseCase,
    private val getEmployeesUseCase: GetEmployeesUseCase,
    private val getProductsInventoryUseCase: GetProductsInventoryUseCase,
    private val getFinishedProductsUseCase: GetFinishedProductsUseCase,
    private val getProductsByLastStepUseCase: GetProductsByLastStepUseCase,
    private val getProductsByStepEmployeeDayUseCase: GetProductsByStepEmployeeDayUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductUiState())
    val uiState: StateFlow<ProductUiState> = _uiState

    private val _events = MutableSharedFlow<ProductEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ProductEvent> = _events

    // ---------- Загрузка продукта по серийному номеру (вход из QR) ----------

    fun loadProduct(serialNumber: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { getProductUseCase(serialNumber) }
                .onSuccess { product ->
                    if (product == null) {
                        loadProcesses()
                        _events.emit(ProductEvent.NavigateToNewProduct)
                    } else {
                        setProduct(product)
                    }
                }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun setProduct(product: Product) {
        val selected = product.steps.firstOrNull { it.status != StepStatus.DONE }
            ?: product.steps.lastOrNull()

        _uiState.update {
            it.copy(
                product = product,
                selectedStep = selected,
            )
        }
        viewModelScope.launch { _events.emit(ProductEvent.NavigateToProduct) }
    }

    fun selectStep(step: Step) {
        _uiState.update { it.copy(selectedStep = step) }
    }

    // ---------- Создание нового продукта ----------

    fun createProduct(serialNumber: String, processId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching { createProductUseCase(serialNumber, processId) }
                .onSuccess { setProduct(it) }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    // ---------- Смена статуса ----------

    fun changeStatus(productId: Long, status: ProductStatus) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching { changeProductStatusUseCase(productId, status) }
                .onSuccess { setProduct(it) }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    // ---------- Смена процесса ----------

    fun changeProcess(productId: Long, newProcessId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching { changeProductProcessUseCase(productId, newProcessId) }
                .onSuccess { setProduct(it) }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    // ---------- Шаги ----------

    fun closeStep(step: Step) {
        if (step.id == 0) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching { closeStepUseCase(step.id) }
                .onSuccess { product ->
                    val updatedStep = product.steps
                        .find { it.definition.order == step.definition.order }
                    _uiState.update {
                        it.copy(
                            product = product,
                            selectedStep = updatedStep,
                        )
                    }
                }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    fun changeStepPerformer(stepId: Int, newEmployeeId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching { changeStepPerformerUseCase(stepId, newEmployeeId) }
                .onSuccess { _uiState.update { state -> state.copy(product = it) } }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    // ---------- Вспомогательные списки ----------

    fun loadProcesses() {
        viewModelScope.launch {
            runCatching { getProcessesUseCase() }
                .onSuccess { _uiState.update { state -> state.copy(processes = it) } }
                .onFailure { emitError(it) }
        }
    }

    fun loadEmployees() {
        viewModelScope.launch {
            runCatching { getEmployeesUseCase() }
                .onSuccess { _uiState.update { state -> state.copy(employees = it) } }
                .onFailure { emitError(it) }
        }
    }

    fun loadProductsInventory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { getProductsInventoryUseCase() }
                .onSuccess { _uiState.update { state -> state.copy(productsInventory = it) } }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun loadAvailableProductsForPackaging() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { getFinishedProductsUseCase() }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(availableProductsForPackaging = it)
                    }
                }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun loadProductsByLastStep(processId: Int, stepDefinitionId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, productsInventoryByProcess = emptyList()) }
            runCatching { getProductsByLastStepUseCase(processId, stepDefinitionId) }
                .onSuccess {
                    _uiState.update { state -> state.copy(productsInventoryByProcess = it) }
                }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun loadProductsByStepEmployeeDay(
        stepDefinitionId: Int,
        day: String,
        employeeId: Int,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, productsInventoryByProcess = emptyList()) }
            runCatching {
                getProductsByStepEmployeeDayUseCase(stepDefinitionId, day, employeeId)
            }
                .onSuccess {
                    _uiState.update { state -> state.copy(productsInventoryByProcess = it) }
                }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // ---------- Очистка ----------

    fun clearProduct() {
        _uiState.update { it.copy(product = null, selectedStep = null) }
    }

    // ---------- Вспомогательное ----------

    private suspend fun emitError(e: Throwable) {
        _events.emit(ProductEvent.ShowError(e.message ?: UNKNOWN_ERROR))
    }

    companion object {
        private const val UNKNOWN_ERROR = "Неизвестная ошибка"
    }
}