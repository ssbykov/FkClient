package ru.faserkraft.client.presentation.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.faserkraft.client.domain.model.OrderItem
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.usecase.order.AddPackagingToOrderUseCase
import ru.faserkraft.client.domain.usecase.order.CloseOrderUseCase
import ru.faserkraft.client.domain.usecase.order.CreateOrderUseCase
import ru.faserkraft.client.domain.usecase.order.DeleteOrderUseCase
import ru.faserkraft.client.domain.usecase.order.DetachPackagingFromOrderUseCase
import ru.faserkraft.client.domain.usecase.order.GetOrderUseCase
import ru.faserkraft.client.domain.usecase.order.GetOrdersUseCase
import ru.faserkraft.client.domain.usecase.order.UpdateOrderItemsUseCase
import ru.faserkraft.client.domain.usecase.order.UpdateOrderUseCase
import ru.faserkraft.client.domain.usecase.process.GetProcessesUseCase
import ru.faserkraft.client.presentation.base.toErrorMessage
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val getOrdersUseCase: GetOrdersUseCase,
    private val getOrderUseCase: GetOrderUseCase,
    private val createOrderUseCase: CreateOrderUseCase,
    private val updateOrderUseCase: UpdateOrderUseCase,
    private val updateOrderItemsUseCase: UpdateOrderItemsUseCase,
    private val closeOrderUseCase: CloseOrderUseCase,
    private val deleteOrderUseCase: DeleteOrderUseCase,
    private val addPackagingToOrderUseCase: AddPackagingToOrderUseCase,
    private val detachPackagingFromOrderUseCase: DetachPackagingFromOrderUseCase,
    private val getProcessesUseCase: GetProcessesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState

    private val _events = Channel<OrderEvent>()
    val events = _events.receiveAsFlow()

    // ---------- Список заказов ----------

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { getOrdersUseCase() }
                .onSuccess { _uiState.update { state -> state.copy(orders = it) } }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // ---------- Конкретный заказ ----------

    fun loadOrder(orderId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { getOrderUseCase(orderId) }
                .onSuccess { _uiState.update { state -> state.copy(currentOrder = it) } }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // ---------- Создание ----------

    fun loadProcesses() {
        viewModelScope.launch {
            runCatching { getProcessesUseCase() }
                .onSuccess { _uiState.update { state -> state.copy(processes = it) } }
                .onFailure { emitError(it) }
        }
    }

    fun createOrder(
        contractNumber: String,
        contractDate: String,
        plannedShipmentDate: String,
        items: List<OrderItem>
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching {
                val order = createOrderUseCase(contractNumber, contractDate, plannedShipmentDate)
                updateOrderItemsUseCase(order.id, items)
            }
                .onSuccess {
                    loadOrders()
                    _events.send(OrderEvent.OrderCreated)
                }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    // ---------- Обновление ----------

    fun updateOrder(
        orderId: Int,
        contractNumber: String,
        contractDate: String,
        plannedShipmentDate: String,
        items: List<OrderItem>
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching {
                updateOrderUseCase(orderId, contractNumber, contractDate, plannedShipmentDate)
                updateOrderItemsUseCase(orderId, items)
            }
                .onSuccess { updatedOrder ->
                    _uiState.update { state -> state.copy(currentOrder = updatedOrder) }
                    loadOrders()
                    _events.send(OrderEvent.OrderUpdated)
                }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    // ---------- Закрытие ----------

    fun requestCloseOrder(orderId: Int) {
        viewModelScope.launch {
            val order = uiState.value.orders.firstOrNull { it.id == orderId }
            if (order == null) {
                _events.send(OrderEvent.ShowError("Не удалось найти заказ"))
                return@launch
            }

            // Ищем серийники упаковок, где есть хотя бы один продукт со статусом не NORMAL
            val invalidPackagingSerials = order.packaging
                .filter { packaging ->
                    packaging.products.any { product ->
                        product.status != ProductStatus.NORMAL
                    }
                }
                .map { it.serialNumber }

            if (invalidPackagingSerials.isNotEmpty()) {
                _events.send(OrderEvent.CloseOrderDenied(invalidPackagingSerials))
            } else {
                _events.send(
                    OrderEvent.ConfirmCloseOrder(
                        orderId = order.id,
                        contractNumber = order.contractNumber
                    )
                )
            }
        }
    }

    fun closeOrder(orderId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching { closeOrderUseCase(orderId) }
                .onSuccess { order ->
                    _uiState.update { it.copy(currentOrder = order) }
                    loadOrders()
                    _events.send(OrderEvent.OrderClosed)
                }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    // ---------- Удаление ----------

    fun deleteOrder(orderId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching { deleteOrderUseCase(orderId) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            currentOrder = if (state.currentOrder?.id == orderId) null
                            else state.currentOrder
                        )
                    }
                    loadOrders()
                    _events.send(OrderEvent.OrderDeleted)
                }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    // ---------- Упаковки в заказе ----------

    fun addPackagingToOrder(orderId: Int, packagingIds: List<Int>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching { addPackagingToOrderUseCase(orderId, packagingIds) }
                .onSuccess {
                    loadOrders()
                    loadOrder(orderId)
                    _events.send(OrderEvent.PackagingAdded)
                }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    fun detachPackagingFromOrder(orderId: Int, packagingIds: List<Int>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching { detachPackagingFromOrderUseCase(packagingIds) }
                .onSuccess {
                    loadOrders()
                    loadOrder(orderId)
                }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    fun requestAddPackaging(orderId: Int, selectedPackaging: List<PackagingShipmentUiItem>) {
        viewModelScope.launch {
            if (selectedPackaging.isEmpty()) {
                _events.send(OrderEvent.ShowError("Вы не выбрали ни одной упаковки для добавления"))
                return@launch
            }

            val invalidSerials = selectedPackaging
                .filter { it.hasNonNormalProducts }
                .map { it.serialNumber }

            if (invalidSerials.isNotEmpty()) {
                _events.send(OrderEvent.AddPackagingDenied(invalidSerials))
            } else {
                _events.send(
                    OrderEvent.ConfirmAddPackaging(
                        orderId = orderId,
                        packagingIds = selectedPackaging.map { it.id },
                        packagingCount = selectedPackaging.size
                    )
                )
            }
        }
    }

    // ---------- Вспомогательное ----------

    private suspend fun emitError(e: Throwable) {
        _events.send(OrderEvent.ShowError(e.toErrorMessage()))
    }
}