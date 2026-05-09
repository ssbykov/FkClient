package ru.faserkraft.client.data.mapper

import ru.faserkraft.client.domain.model.Packaging
import ru.faserkraft.client.data.dto.PackagingDto

fun PackagingDto.toDomain(): Packaging = Packaging(
    id = id,
    serialNumber = serialNumber,
    performedBy = performedBy?.toDomain(),
    performedAt = performedAt,
    orderId = orderId,
    products = products?.map { it.toDomain() } ?: emptyList(),
)
