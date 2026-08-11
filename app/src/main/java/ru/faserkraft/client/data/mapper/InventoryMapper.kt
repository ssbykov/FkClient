package ru.faserkraft.client.data.mapper


import ru.faserkraft.client.data.dto.InventoryDto
import ru.faserkraft.client.data.dto.InventoryItemDto
import ru.faserkraft.client.data.dto.ProductInventoryCompareItemDto
import ru.faserkraft.client.domain.model.Inventory
import ru.faserkraft.client.domain.model.InventoryItem
import ru.faserkraft.client.domain.model.ProductInventoryCompareItem
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException


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

fun ProductInventoryCompareItemDto.toDomain(): ProductInventoryCompareItem {

    val parsedDate = performedAt?.let { dateString ->
        try {
            ZonedDateTime.parse(dateString).toLocalDateTime()
        } catch (e: DateTimeParseException) {
            null
        }
    }

    return ProductInventoryCompareItem(
        id = id,
        serialNumber = serialNumber,
        status = status?.toDomain(),
        inventoryStepDefinition = inventoryStepDefinition?.toDomain(),
        accountingStepDefinition = accountingStepDefinition?.toDomain(),
        performedAt = parsedDate
    )
}


fun List<ProductInventoryCompareItemDto>.toDomain(): List<ProductInventoryCompareItem> =
    this.map { it.toDomain() }