package ru.faserkraft.client.data.mapper

import ru.faserkraft.client.domain.model.Order
import ru.faserkraft.client.domain.model.OrderItem
import ru.faserkraft.client.data.dto.OrderDto
import ru.faserkraft.client.data.dto.OrderItemCreateDto
import ru.faserkraft.client.data.dto.OrderItemDto

fun OrderItemDto.toDomain(): OrderItem = OrderItem(
    id = id,
    quantity = quantity,
    workProcess = workProcess.toDomain(),
)

fun OrderDto.toDomain(): Order = Order(
    id = id,
    contractNumber = contractNumber,
    contractDate = contractDate,
    plannedShipmentDate = plannedShipmentDate,
    shipmentDate = shipmentDate,
    shipmentBy = shipmentBy?.toDomain(),
    items = items?.map { it.toDomain() } ?: emptyList(),
    packaging = packaging?.map { it.toDomain() } ?: emptyList(),
)


fun OrderItem.toCreateDto(): OrderItemCreateDto = OrderItemCreateDto(
    processId = workProcess.id,
    quantity = quantity,
)