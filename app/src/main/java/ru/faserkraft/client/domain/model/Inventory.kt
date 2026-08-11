package ru.faserkraft.client.domain.model

import java.time.LocalDateTime

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


data class ProductInventoryCompareItem(
    val id: Int?,
    val serialNumber: String,
    val status: ProductStatus?,
    val inventoryStepDefinition: StepDefinitionWithProcess?,
    val accountingStepDefinition: StepDefinitionWithProcess?,
    val performedAt: LocalDateTime?
)