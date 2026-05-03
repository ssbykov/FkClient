package ru.faserkraft.client.presentation.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.faserkraft.client.domain.model.OrderItem
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

    private val _events = MutableSharedFlow<OrderEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<OrderEvent> = _events

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

    fun clearCurrentOrder() {
        _uiState.update { it.copy(currentOrder = null) }
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
                    _events.emit(OrderEvent.OrderCreated)
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
                    _events.emit(OrderEvent.OrderUpdated)
                }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    // ---------- Закрытие ----------

    fun closeOrder(orderId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching { closeOrderUseCase(orderId) }
                .onSuccess { order ->
                    _uiState.update { it.copy(currentOrder = order) }
                    loadOrders()
                    _events.emit(OrderEvent.OrderClosed)
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
                    _events.emit(OrderEvent.OrderDeleted)
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
                    _events.emit(OrderEvent.PackagingAdded)
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

    // ---------- Вспомогательное ----------

    private suspend fun emitError(e: Throwable) {
        _events.emit(OrderEvent.ShowError(e.toErrorMessage()))
    }
}