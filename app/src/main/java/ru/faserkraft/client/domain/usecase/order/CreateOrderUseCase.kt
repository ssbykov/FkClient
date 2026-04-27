package ru.faserkraft.client.domain.usecase.order

import ru.faserkraft.client.domain.model.Order
import ru.faserkraft.client.domain.repository.OrderRepository
import javax.inject.Inject

/**
 * Use Case для создания заказа
 */
class CreateOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(number: String): Result<Order> {
        return orderRepository.createOrder(number)
    }
}

