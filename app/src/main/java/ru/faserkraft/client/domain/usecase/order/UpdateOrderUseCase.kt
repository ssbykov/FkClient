package ru.faserkraft.client.domain.usecase.order

import ru.faserkraft.client.domain.model.Order
import ru.faserkraft.client.domain.repository.OrderRepository
import javax.inject.Inject

class UpdateOrderUseCase @Inject constructor(
    private val repository: OrderRepository
) {
    suspend operator fun invoke(
        orderId: Int,
        contractNumber: String,
        contractDate: String,
        plannedShipmentDate: String,
    ): Order = repository.updateOrder(orderId, contractNumber, contractDate, plannedShipmentDate)
}