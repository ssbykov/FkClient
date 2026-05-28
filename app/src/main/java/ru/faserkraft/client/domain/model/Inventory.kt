package ru.faserkraft.client.domain.model

data class Inventory(
    val id: Int,
    val createdAt: String,
    val completedAt: String?,
    val createdById: Int,
    val itemCount: Int,
) {
    val isOpen: Boolean get() = completedAt == null
}

data class InventoryItem(
    val id: Int,
    val inventoryId: Int,
    val serialNumber: String,
    val stepDefinition: StepDefinitionWithProcess,
    val scannedAt: String,
)

data class ProductInventoryItem(
    val serialNumber: String,
    val stepDefinition: StepDefinitionWithProcess,
)

data class InventoryCompareResult(
    val dbCount: Int,
    val scannedCount: Int,
    val matched: List<ProductInventoryItem>,
    val missing: List<ProductInventoryItem>,
    val unexpected: List<ProductInventoryItem>,
) {

    val stepDefinition: StepDefinitionWithProcess
        get() = matched.firstOrNull()?.stepDefinition
            ?: missing.firstOrNull()?.stepDefinition
            ?: unexpected.first().stepDefinition

    val hasDiff: Boolean get() = missing.isNotEmpty() || unexpected.isNotEmpty()
    val diff: Int get() = scannedCount - dbCount
}