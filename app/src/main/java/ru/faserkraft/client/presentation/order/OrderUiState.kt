package ru.faserkraft.client.presentation.order

import ru.faserkraft.client.domain.model.Order

data class OrderUiState(
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val orders: List<Order> = emptyList(),
    val currentOrder: Order? = null,
)