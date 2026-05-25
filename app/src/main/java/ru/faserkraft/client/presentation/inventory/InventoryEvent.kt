package ru.faserkraft.client.presentation.inventory

sealed interface InventoryEvent {
    data class ShowError(val message: String) : InventoryEvent
    data object NavigateToScan : InventoryEvent
    data object NavigateToResults : InventoryEvent
    data object InventoryClosed : InventoryEvent

    // Найден продукт — показать диалог подтверждения
    data object ShowConfirmDialog : InventoryEvent

    // Позиция добавлена/обновлена успешно
    data object ItemUpserted : InventoryEvent

    // Штрихкод уже был отсканирован (дубликат)
    data class ShowDuplicateWarning(val serialNumber: String) : InventoryEvent
}