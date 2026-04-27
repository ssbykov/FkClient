package ru.faserkraft.client.data.dto

import ru.faserkraft.client.domain.model.FinishedProduct
import ru.faserkraft.client.domain.model.Packaging

/**
 * Преобразование DTO в Domain модель для упаковки
 */
fun PackagingDto.toDomain(): Packaging {
    return Packaging(
        id = id,
        serialNumber = serialNumber,
        products = products.map { it.toDomain() },
        createdDate = performedAt,
        lastModifiedDate = performedAt
    )
}

fun FinishedProductDto.toDomain(): FinishedProduct {
    return FinishedProduct(
        id = id.toLong(),
        serialNumber = serialNumber,
        processId = processId,
        status = status
    )
}
