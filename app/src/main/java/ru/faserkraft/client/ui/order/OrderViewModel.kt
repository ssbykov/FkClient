package ru.faserkraft.client.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.faserkraft.client.domain.model.Order
import ru.faserkraft.client.domain.model.UiState
import ru.faserkraft.client.domain.usecase.order.GetOrdersUseCase
import ru.faserkraft.client.domain.usecase.order.CreateOrderUseCase
import ru.faserkraft.client.domain.usecase.order.CloseOrderUseCase
import javax.inject.Inject

/**
 * ViewModel для работы с заказами
 */
@HiltViewModel
class OrderViewModel @Inject constructor(
    private val getOrdersUseCase: GetOrdersUseCase,
    private val createOrderUseCase: CreateOrderUseCase,
    private val closeOrderUseCase: CloseOrderUseCase
) : ViewModel() {

    private val _ordersState = MutableStateFlow<UiState<List<Order>>>(UiState.Idle)
    val ordersState: StateFlow<UiState<List<Order>>> = _ordersState

    private val _currentOrderState = MutableStateFlow<UiState<Order>>(UiState.Idle)
    val currentOrderState: StateFlow<UiState<Order>> = _currentOrderState

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState

    /**
     * Получить все заказы
     */
    fun getOrders() {
        viewModelScope.launch {
            _ordersState.value = UiState.Loading
            getOrdersUseCase()
                .onSuccess { orders ->
                    _ordersState.value = UiState.Success(orders)
                }
                .onFailure { exception ->
                    _ordersState.value = UiState.Error(exception)
                }
        }
    }

    /**
     * Получить конкретный заказ
     */
    fun getOrder(orderId: Int) {
        viewModelScope.launch {
            _currentOrderState.value = UiState.Loading
            getOrdersUseCase.getById(orderId)
                .onSuccess { order ->
                    _currentOrderState.value = UiState.Success(order)
                }
                .onFailure { exception ->
                    _currentOrderState.value = UiState.Error(exception)
                }
        }
    }

    /**
     * Создать новый заказ
     */
    fun createOrder(number: String) {
        viewModelScope.launch {
            _actionState.value = ActionState.InProgress
            createOrderUseCase(number)
                .onSuccess { order ->
                    _currentOrderState.value = UiState.Success(order)
                    _actionState.value = ActionState.Success("Заказ создан")
                    getOrders()
                }
                .onFailure { exception ->
                    _actionState.value = ActionState.Error(exception)
                }
        }
    }

    /**
     * Закрыть заказ
     */
    fun closeOrder(orderId: Int) {
        viewModelScope.launch {
            _actionState.value = ActionState.InProgress
            closeOrderUseCase(orderId)
                .onSuccess { closedOrder ->
                    _currentOrderState.value = UiState.Success(closedOrder)
                    _actionState.value = ActionState.Success("Заказ закрыт")
                    getOrders()
                }
                .onFailure { exception ->
                    _actionState.value = ActionState.Error(exception)
                }
        }
    }

    /**
     * Сбросить текущий заказ
     */
    fun clearCurrentOrder() {
        _currentOrderState.value = UiState.Idle
    }

    fun clearOrders() {
        _ordersState.value = UiState.Idle
        _currentOrderState.value = UiState.Idle
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
