package ru.faserkraft.client.presentation.order

sealed interface OrderEvent {
    data class ShowError(val message: String) : OrderEvent
    data class ConfirmCloseOrder(
        val orderId: Int,
        val contractNumber: String
    ) : OrderEvent

    data class CloseOrderDenied(
        val invalidPackagingSerials: List<String>
    ) : OrderEvent
    data class ConfirmAddPackaging(
        val orderId: Int,
        val packagingIds: List<Int>,
        val packagingCount: Int
    ) : OrderEvent

    data class AddPackagingDenied(
        val invalidPackagingSerials: List<String>
    ) : OrderEvent
    data object OrderClosed : OrderEvent
    data object OrderDeleted : OrderEvent
    data object OrderUpdated : OrderEvent
    data object OrderCreated : OrderEvent
    data object PackagingAdded : OrderEvent
}