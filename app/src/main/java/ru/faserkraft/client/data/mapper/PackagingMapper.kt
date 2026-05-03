package ru.faserkraft.client.data.mapper

import ru.faserkraft.client.domain.model.Packaging
import ru.faserkraft.client.dto.PackagingCreateDto
import ru.faserkraft.client.dto.PackagingDto

fun PackagingDto.toDomain(): Packaging = Packaging(
    id = id,
    serialNumber = serialNumber,
    performedBy = performedBy?.toDomain(),
    performedAt = performedAt,
    orderId = orderId,
    products = products?.map { it.toDomain() } ?: emptyList(),
)

fun Packaging.toCreateDto(productIds: List<Int>): PackagingCreateDto = PackagingCreateDto(
    serialNumber = serialNumber,
    products = productIds,
)