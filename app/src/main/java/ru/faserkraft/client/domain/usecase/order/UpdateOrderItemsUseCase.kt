package ru.faserkraft.client.domain.usecase.order

import ru.faserkraft.client.domain.model.Order
import ru.faserkraft.client.domain.model.OrderItem
import ru.faserkraft.client.domain.repository.OrderRepository
import javax.inject.Inject

class UpdateOrderItemsUseCase @Inject constructor(
    private val repository: OrderRepository
) {
    suspend operator fun invoke(orderId: Int, items: List<OrderItem>): Order =
        repository.updateOrderItems(orderId, items)
}