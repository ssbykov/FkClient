package ru.faserkraft.client.data.dto

import ru.faserkraft.client.domain.model.Order
import ru.faserkraft.client.domain.model.OrderItem
import ru.faserkraft.client.domain.model.Packaging
import ru.faserkraft.client.domain.model.Process as DomainProcess

/**
 * Преобразование DTO в Domain модель для заказов
 */
fun OrderDto.toDomain(): Order {
    return Order(
        id = id,
        contractNumber = contractNumber,
        contractDate = contractDate,
        plannedShipmentDate = plannedShipmentDate,
        shipmentDate = shipmentDate,
        status = status,
        items = items.map { it.toDomain() },
        packaging = packaging.map { it.toDomain() },
        createdDate = createdDate,
        lastModifiedDate = lastModifiedDate
    )
}

fun OrderItemDto.toDomain(): OrderItem {
    return OrderItem(
        id = id,
        productId = productId,
        quantity = quantity,
        status = status,
        workProcess = workProcess.toDomain()
    )
}

fun ProcessDto.toDomain(): DomainProcess {
    return DomainProcess(
        id = id,
        name = name,
        description = description,
        steps = steps.map { it.toDomain() }
    )
}
