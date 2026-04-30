package ru.faserkraft.client.data.mapper

import ru.faserkraft.client.domain.model.FinishedProduct
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.model.ProductsInventory
import ru.faserkraft.client.domain.model.Step
import ru.faserkraft.client.domain.model.StepStatus
import ru.faserkraft.client.dto.FinishedProductDto
import ru.faserkraft.client.dto.ProductCreateDto
import ru.faserkraft.client.dto.ProductDto
import ru.faserkraft.client.dto.ProductStatusDto
import ru.faserkraft.client.dto.ProductsInventoryDto
import ru.faserkraft.client.dto.StepDto

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
    packagingId = packagingId,
    status = status.toDomain(),
    steps = steps.map { it.toDomain() },
)

fun ProductStatusDto.toDomain(): ProductStatus = when (this) {
    ProductStatusDto.NORMAL -> ProductStatus.NORMAL
    ProductStatusDto.REWORK -> ProductStatus.REWORK
    ProductStatusDto.SCRAP -> ProductStatus.SCRAP
}

fun ProductStatus.toDto(): ProductStatusDto = when (this) {
    ProductStatus.NORMAL -> ProductStatusDto.NORMAL
    ProductStatus.REWORK -> ProductStatusDto.REWORK
    ProductStatus.SCRAP -> ProductStatusDto.SCRAP
}

fun FinishedProductDto.toDomain(): FinishedProduct = FinishedProduct(
    id = id,
    serialNumber = serialNumber,
    process = process.toDomain(),
)

fun ProductsInventoryDto.toDomain(): ProductsInventory = ProductsInventory(
    processId = processId,
    processName = processName,
    stepDefinitionId = stepDefinitionId,
    stepName = stepName,
    stepNameGenitive = stepNameGenitive,
    count = count,
)

fun Product.toCreateDto(processId: Int): ProductCreateDto = ProductCreateDto(
    processId = processId,
    serialNumber = serialNumber,
)

fun String.toStepStatus(): StepStatus = when (lowercase()) {
    "done" -> StepStatus.DONE
    else -> StepStatus.PENDING
}

fun ProductStatus.toDisplayString(): String = when (this) {
    ProductStatus.NORMAL -> "Норма"
    ProductStatus.REWORK -> "Ремонт"
    ProductStatus.SCRAP -> "Брак"
}