package ru.faserkraft.client.domain.usecase.order

import ru.faserkraft.client.domain.model.Order
import ru.faserkraft.client.domain.repository.OrderRepository
import javax.inject.Inject

/**
 * Use Case для закрытия заказа
 */
class CloseOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(orderId: Int): Result<Order> {
        return orderRepository.closeOrder(orderId)
    }
}

