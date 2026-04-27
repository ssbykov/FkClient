package ru.faserkraft.client.domain.usecase.order

import ru.faserkraft.client.domain.model.Order
import ru.faserkraft.client.domain.repository.OrderRepository
import javax.inject.Inject

/**
 * Use Case для получения заказов
 */
class GetOrdersUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    /**
     * Получить все заказы
     */
    suspend operator fun invoke(): Result<List<Order>> {
        return orderRepository.getAllOrders()
    }

    /**
     * Получить конкретный заказ
     */
    suspend fun getById(orderId: Int): Result<Order> {
        return orderRepository.getOrderById(orderId)
    }
}

