package ru.faserkraft.client.domain.model

data class Inventory(
    val id: Int,
    val createdAt: String,
    val completedAt: String?,
    val createdById: Int,
) {
    val isOpen: Boolean get() = completedAt == null
}

data class InventoryItem(
    val id: Int,
    val inventoryId: Int,
    val serialNumber: String,
    val stepDefinitionId: Int,
    val stepDefinition: StepDefinition,
    val scannedAt: String,
)

data class InventoryCompareResult(
    val stepDefinitionId: Int,
    val stepName: String?,
    val processId: Int?,
    val dbCount: Int,
    val scannedCount: Int,
    val matched: List<String>,
    val missing: List<String>,
    val unexpected: List<String>,
) {
    val hasDiff: Boolean get() = missing.isNotEmpty() || unexpected.isNotEmpty()
    val diff: Int get() = scannedCount - dbCount
}