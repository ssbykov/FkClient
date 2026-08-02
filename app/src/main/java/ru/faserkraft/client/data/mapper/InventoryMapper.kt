package ru.faserkraft.client.data.mapper


import ru.faserkraft.client.data.dto.InventoryCompareResultDto
import ru.faserkraft.client.data.dto.InventoryDto
import ru.faserkraft.client.data.dto.InventoryItemDto
import ru.faserkraft.client.data.dto.ProductInventoryItemDto
import ru.faserkraft.client.domain.model.Inventory
import ru.faserkraft.client.domain.model.InventoryCompareResult
import ru.faserkraft.client.domain.model.InventoryItem
import ru.faserkraft.client.domain.model.ProductInventoryItem


fun InventoryDto.toDomain(): Inventory = Inventory(
    id = id,
    createdAt = createdAt,
    completedAt = completedAt,
    createdById = createdById,
    itemCount = itemCount,
)

fun InventoryItemDto.toDomain(): InventoryItem = InventoryItem(
    id = id,
    inventoryId = inventoryId,
    serialNumber = serialNumber,
    stepDefinition = stepDefinition.toDomain(),
    scannedAt = scannedAt,
)

fun ProductInventoryItemDto.toDomain(): ProductInventoryItem = ProductInventoryItem(
    id = id,
    serialNumber = serialNumber,
    status = status.toDomain(),
    stepDefinition = stepDefinition.toDomain(),
    performedAt = performedAt,
)

fun InventoryCompareResultDto.toDomain(): InventoryCompareResult = InventoryCompareResult(
    dbCount = dbCount,
    scannedCount = scannedCount,
    matched = matched.map { it.toDomain() },
    missing = missing.map { it.toDomain() },
    unexpected = unexpected.map { it.toDomain() },
)