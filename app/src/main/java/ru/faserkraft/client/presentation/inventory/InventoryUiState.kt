package ru.faserkraft.client.presentation.inventory

import ru.faserkraft.client.domain.model.Inventory
import ru.faserkraft.client.domain.model.InventoryItem
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.ProductInventoryCompareItem
import ru.faserkraft.client.domain.model.StepDefinition

data class InventoryUiState(
    val inventories: List<Inventory> = emptyList(),
    val currentInventory: Inventory? = null,
    val currentInventoryItems: List<InventoryItem> = emptyList(),
    val pendingProduct: Product? = null,
    val availableSteps: List<StepDefinition> = emptyList(),
    val preselectedStepId: Int? = null,
    val compareResults: List<ProductInventoryCompareItem> = emptyList(),
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
) {
    val currentInventoryItemCount: Int get() = currentInventoryItems.size
}