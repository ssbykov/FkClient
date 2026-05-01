package ru.faserkraft.client.presentation.order

sealed interface OrderEvent {
    data class ShowError(val message: String) : OrderEvent
    data object OrderDeleted : OrderEvent
    data object OrderClosed : OrderEvent
    data object OrderUpdated : OrderEvent
    data object OrderCreated : OrderEvent
    data object PackagingAdded : OrderEvent
}