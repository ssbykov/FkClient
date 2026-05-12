package ru.faserkraft.client.data.mapper

import ru.faserkraft.client.domain.model.ProductShort
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.model.ProductsInventory
import ru.faserkraft.client.domain.model.Step
import ru.faserkraft.client.domain.model.StepStatus
import ru.faserkraft.client.data.dto.ProductShortDto
import ru.faserkraft.client.data.dto.ProductDto
import ru.faserkraft.client.data.dto.ProductStatusDto
import ru.faserkraft.client.data.dto.ProductsInventoryDto
import ru.faserkraft.client.data.dto.StepDto

fun StepDto.toDomain(): Step = Step(
    id = id,
    productId = productId,
    definition = stepDefinition.toDomain(),
    status = when (status.lowercase()) {
        "done" -> StepStatus.DONE
        else -> StepStatus.PENDING
    },
    performedBy = performedBy?.toDomain(),
    performedAt = performedAt,
)

fun ProductDto.toDomain(): Product = Product(
    id = id,
    serialNumber = serialNumber,
    process = process.toDomain(),
    createdAt = createdAt,
    packagingSerialNumber = packaging?.serialNumber,
    status = status.toDomain(),
    steps = steps.map { it.toDomain() },
)

fun ProductStatusDto.toDomain(): ProductStatus = when (this) {
    ProductStatusDto.NORMAL -> ProductStatus.NORMAL
    ProductStatusDto.REWORK -> ProductStatus.REWORK
    ProductStatusDto.SCRAP -> ProductStatus.SCRAP
}

fun ProductStatus.toDto(): String = when (this) {
    ProductStatus.NORMAL -> "normal"
    ProductStatus.REWORK -> "rework"
    ProductStatus.SCRAP -> "scrap"
}

fun ProductShortDto.toDomain(): ProductShort = ProductShort(
    id = id,
    serialNumber = serialNumber,
    process = process.toDomain(),
    status = status.toDomain(),
)

fun ProductsInventoryDto.toDomain(): ProductsInventory = ProductsInventory(
    processId = processId,
    processName = processName,
    stepDefinitionId = stepDefinitionId,
    stepName = stepName,
    stepNameGenitive = stepNameGenitive,
    count = count,
)
