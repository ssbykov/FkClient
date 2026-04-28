package ru.faserkraft.client.domain.usecase.order

import ru.faserkraft.client.domain.model.Order
import ru.faserkraft.client.domain.repository.OrderRepository
import javax.inject.Inject

class GetOrdersUseCase @Inject constructor(
    private val repository: OrderRepository
) {
    suspend operator fun invoke(): List<Order> =
        repository.getAllOrders()
}