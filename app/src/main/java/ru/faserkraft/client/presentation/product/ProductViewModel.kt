package ru.faserkraft.client.presentation.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.faserkraft.client.auth.AppAuth
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.model.ProductsInventory
import ru.faserkraft.client.domain.model.Step
import ru.faserkraft.client.domain.model.StepStatus
import ru.faserkraft.client.domain.model.UserRole
import ru.faserkraft.client.domain.usecase.employee.GetEmployeesUseCase
import ru.faserkraft.client.domain.usecase.process.GetProcessesUseCase
import ru.faserkraft.client.domain.usecase.product.ChangeProductProcessUseCase
import ru.faserkraft.client.domain.usecase.product.ChangeProductStatusUseCase
import ru.faserkraft.client.domain.usecase.product.CreateProductUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductsByLastStepUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductsByStatusUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductsInventoryUseCase
import ru.faserkraft.client.domain.usecase.step.ChangeStepPerformerUseCase
import ru.faserkraft.client.domain.usecase.step.CloseStepUseCase
import ru.faserkraft.client.presentation.app.AppSessionCoordinator
import ru.faserkraft.client.presentation.app.AppSessionEvent
import ru.faserkraft.client.presentation.base.toErrorMessage
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
    private val getProductsByLastStepUseCase: GetProductsByLastStepUseCase,
    private val appAuth: AppAuth,
    private val sessionCoordinator: AppSessionCoordinator,
    private val getProductsByStatusUseCase: GetProductsByStatusUseCase,
) : ViewModel() {

    private val initialRole: UserRole? = appAuth.getRegistrationData()?.role

    private val _uiState = MutableStateFlow(
        ProductUiState(userRole = initialRole)
    )
    val uiState: StateFlow<ProductUiState> = _uiState

    private val _events = Channel<ProductEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        observeSessionEvents()
    }

    // ---------- Session ----------

    private fun observeSessionEvents() {
        viewModelScope.launch {
            sessionCoordinator.events.collect { event ->
                when (event) {
                    AppSessionEvent.Logout -> clearStateOnLogout()
                }
            }
        }
    }

    private fun clearStateOnLogout() {
        _uiState.value = ProductUiState(userRole = null)
    }

    private fun currentRole(): UserRole? {
        return _uiState.value.userRole ?: appAuth.getRegistrationData()?.role
    }

    // ---------- UI actions ----------

    fun onChangeStatusClicked() {
        if (!_uiState.value.userRole.canEditProduct()) return
        _uiState.value.product ?: return

        viewModelScope.launch {
            _events.send(
                ProductEvent.ShowConfirmationDialog(
                    title = "Изменение статуса",
                    message = "Вы уверены?",
                    actionType = ConfirmationActionType.CHANGE_STATUS,
                )
            )
        }
    }

    fun onChangeProcessClicked() {
        val product = _uiState.value.product ?: return
        if (!_uiState.value.userRole.canEditProduct() || product.packagingSerialNumber != null) return

        viewModelScope.launch {
            _events.send(
                ProductEvent.ShowConfirmationDialog(
                    title = "Изменение процесса",
                    message = "Вы уверены?",
                    actionType = ConfirmationActionType.CHANGE_PROCESS,
                )
            )
        }
    }

    fun onCloseStepClicked() {
        val state = _uiState.value
        val product = state.product ?: return
        val step = state.selectedStep ?: return
        if (step.id == 0) return

        viewModelScope.launch {
            when {
                product.status == ProductStatus.REWORK ||
                        product.status == ProductStatus.SCRAP -> {
                    _events.send(
                        ProductEvent.ShowError(
                            "Нельзя закрыть этап: продукт в статусе РЕМОНТ или БРАК"
                        )
                    )
                }

                step.status == StepStatus.DONE -> {
                    _events.send(ProductEvent.ShowError("Этап уже выполнен"))
                }

                else -> {
                    _events.send(
                        ProductEvent.ShowConfirmationDialog(
                            title = "Закрыть этап",
                            message = "Вы уверены?",
                            actionType = ConfirmationActionType.CLOSE_STEP,
                            step = step,
                        )
                    )
                }
            }
        }
    }

    fun onDialogConfirmed(actionType: ConfirmationActionType, step: Step?) {
        val product = _uiState.value.product ?: return

        when (actionType) {
            ConfirmationActionType.CHANGE_STATUS -> {
                viewModelScope.launch {
                    _events.send(ProductEvent.NavigateToEditStatus(product.id))
                }
            }

            ConfirmationActionType.CHANGE_PROCESS -> {
                loadProcesses()
                viewModelScope.launch {
                    _events.send(ProductEvent.NavigateToEditProcess(product.id))
                }
            }

            ConfirmationActionType.CLOSE_STEP -> {
                step?.let { closeStep(it) }
            }
        }
    }

    fun onPackagingClicked() {
        val packagingSerial = _uiState.value.product?.packagingSerialNumber ?: return

        viewModelScope.launch {
            _events.send(ProductEvent.NavigateToPackaging(packagingSerial))
        }
    }

    // ---------- Product loading ----------

    fun loadProduct(serialNumber: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            runCatching { getProductUseCase(serialNumber) }
                .onSuccess { product ->
                    if (product == null) {
                        loadProcesses()
                        _uiState.update {
                            it.copy(pendingSerialNumber = serialNumber)
                        }
                        _events.send(ProductEvent.NavigateToNewProduct)
                    } else {
                        updateProductState(product)
                        // Сбрасываем pendingSerialNumber
                        _uiState.update { it.copy(pendingSerialNumber = null) }
                        // Навигируемся только при первичной загрузке
                        _events.send(ProductEvent.NavigateToProduct)
                    }
                }
                .onFailure { emitError(it) }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun loadReworkScrapProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            runCatching {
                getProductsByStatusUseCase(
                    listOf(ProductStatus.REWORK, ProductStatus.SCRAP)
                )
            }
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(
                            reworkScrapProducts = list,
                            userRole = currentRole(),
                        )
                    }
                }
                .onFailure { emitError(it) }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun updateProductState(product: Product) {
        val selected = product.steps.firstOrNull { it.status != StepStatus.DONE }
            ?: product.steps.lastOrNull()

        _uiState.update {
            it.copy(
                product = product,
                selectedStep = selected,
                userRole = currentRole(),
            )
        }
    }

    fun selectStep(step: Step) {
        _uiState.update { it.copy(selectedStep = step) }
    }

    fun selectInventoryItem(item: ProductsInventory) {
        _uiState.update { it.copy(selectedInventoryItem = item) }
    }

    fun selectReworkScrapProduct(processName: String, status: ProductStatus) {
        _uiState.update { state ->
            state.copy(
                selectedScrapReworkItem = ReworkScrapSelection(processName, status)
            )
        }
    }

    // ---------- Create product ----------

    fun createProduct(serialNumber: String, processId: Int) {
        withActionProgress {
            runCatching { createProductUseCase(serialNumber, processId) }
                .onSuccess { product ->
                    updateProductState(product)
                    _uiState.update { it.copy(pendingSerialNumber = null) }
                    // Для нового продукта нам нужна навигация на его экран
                    _events.send(ProductEvent.NavigateToProduct)
                }
                .onFailure { emitError(it) }
        }
    }

    // ---------- Change status ----------

    fun changeStatus(productId: Long, status: ProductStatus) {
        withActionProgress {
            runCatching { changeProductStatusUseCase(productId, status) }
                .onSuccess { updateProductState(it) }
                .onFailure { emitError(it) }
        }
    }

    // ---------- Change process ----------

    fun changeProcess(productId: Long, newProcessId: Int) {
        withActionProgress {
            runCatching { changeProductProcessUseCase(productId, newProcessId) }
                .onSuccess { updateProductState(it) }
                .onFailure { emitError(it) }
        }
    }

    // ---------- Steps ----------

    fun closeStep(step: Step) {
        if (step.id == 0) return

        withActionProgress {
            runCatching { closeStepUseCase(step.id) }
                .onSuccess { product ->
                    val updatedStep = product.steps
                        .find { it.definition.order == step.definition.order }

                    _uiState.update {
                        it.copy(
                            product = product,
                            selectedStep = updatedStep,
                            userRole = currentRole(),
                        )
                    }
                }
                .onFailure { emitError(it) }
        }
    }

    fun changeStepPerformer(stepId: Int, newEmployeeId: Int) {
        withActionProgress {
            runCatching { changeStepPerformerUseCase(stepId, newEmployeeId) }
                .onSuccess { product ->
                    _uiState.update {
                        it.copy(
                            product = product,
                            userRole = currentRole(),
                        )
                    }
                }
                .onFailure { emitError(it) }
        }
    }

    // ---------- Dictionaries ----------

    fun loadProcesses() {
        viewModelScope.launch {
            runCatching { getProcessesUseCase() }
                .onSuccess { processes ->
                    _uiState.update {
                        it.copy(
                            processes = processes,
                            userRole = currentRole(),
                        )
                    }
                }
                .onFailure { emitError(it) }
        }
    }

    fun loadEmployees() {
        viewModelScope.launch {
            runCatching { getEmployeesUseCase() }
                .onSuccess { employees ->
                    _uiState.update {
                        it.copy(
                            employees = employees,
                            userRole = currentRole(),
                        )
                    }
                }
                .onFailure { emitError(it) }
        }
    }

    fun loadProductsInventory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            runCatching { getProductsInventoryUseCase() }
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(
                            productsInventory = list,
                            userRole = currentRole(),
                        )
                    }
                }
                .onFailure { emitError(it) }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun loadProductsByLastStep(processId: Int, stepDefinitionId: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    productsInventoryByProcess = emptyList(),
                )
            }

            runCatching { getProductsByLastStepUseCase(processId, stepDefinitionId) }
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(
                            productsInventoryByProcess = list,
                            userRole = currentRole(),
                        )
                    }
                }
                .onFailure { emitError(it) }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // ---------- Private Helpers ----------

    /**
     * Обертка для всех мутирующих операций (изменение статуса, создание продукта и т.д.).
     * Автоматически управляет флагом isActionInProgress.
     */
    private fun withActionProgress(block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            block()
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    private suspend fun emitError(e: Throwable) {
        _events.send(ProductEvent.ShowError(e.toErrorMessage()))
    }
}

fun UserRole?.canEditProduct(): Boolean {
    return this == UserRole.ADMIN || this == UserRole.MASTER
}