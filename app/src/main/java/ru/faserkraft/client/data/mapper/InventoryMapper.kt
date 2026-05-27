package ru.faserkraft.client.data.mapper


import ru.faserkraft.client.data.dto.InventoryCompareResultDto
import ru.faserkraft.client.data.dto.InventoryDto
import ru.faserkraft.client.data.dto.InventoryItemDto
import ru.faserkraft.client.domain.model.Inventory
import ru.faserkraft.client.domain.model.InventoryCompareResult
import ru.faserkraft.client.domain.model.InventoryItem


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
    stepDefinitionId = stepDefinitionId,
    stepDefinition = stepDefinition.toDomain(),  // из ProcessMapper
    scannedAt = scannedAt,
)

fun InventoryCompareResultDto.toDomain(): InventoryCompareResult = InventoryCompareResult(
    stepDefinitionId = stepDefinitionId,
    stepName = stepName,
    processId = processId,
    dbCount = dbCount,
    scannedCount = scannedCount,
    matched = matched,
    missing = missing,
    unexpected = unexpected,
)