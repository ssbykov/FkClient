package ru.faserkraft.client.presentation.order

import ru.faserkraft.client.domain.model.Order
import ru.faserkraft.client.domain.model.Process

data class OrderUiState(
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val orders: List<Order> = emptyList(),
    val currentOrder: Order? = null,
    val processes: List<Process> = emptyList(),
)