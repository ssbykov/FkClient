package ru.faserkraft.client.presentation.order

/**
 * Одноразовые события (Single Live Events) для модуля заказов.
 */
sealed interface OrderEvent {

    /** Отображение ошибки в виде Snackbar/Toast */
    data class ShowError(val message: String) : OrderEvent

    /** Запрос подтверждения закрытия заказа */
    data class ConfirmCloseOrder(
        val orderId: Int,
        val contractNumber: String
    ) : OrderEvent

    /** Отказ в закрытии заказа из-за некорректных статусов изделий */
    data class CloseOrderDenied(
        val invalidPackagingSerials: List<String>
    ) : OrderEvent

    /** Запрос подтверждения добавления выбранных упаковок в заказ */
    data class ConfirmAddPackaging(
        val orderId: Int,
        val packagingIds: List<Int>,
        val packagingCount: Int
    ) : OrderEvent

    /** Отказ в добавлении упаковок из-за некорректных статусов изделий */
    data class AddPackagingDenied(
        val invalidPackagingSerials: List<String>
    ) : OrderEvent

    /** Успешное закрытие заказа */
    data object OrderClosed : OrderEvent

    /** Успешное удаление заказа */
    data object OrderDeleted : OrderEvent

    /** Успешное обновление данных заказа */
    data object OrderUpdated : OrderEvent

    /** Успешное создание нового заказа */
    data object OrderCreated : OrderEvent

    /** Успешное добавление упаковок в заказ */
    data object PackagingAdded : OrderEvent

    /**
     * Ошибка при отвязке конкретной упаковки от заказа.
     * Передает ID упаковки для точечного точечного обновления элемента (notifyItemChanged).
     */
    data class DetachPackagingFailed(val packagingId: Int) : OrderEvent
}
