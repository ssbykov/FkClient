package ru.faserkraft.client.domain.usecase.order

import ru.faserkraft.client.domain.repository.OrderRepository
import javax.inject.Inject

class DeleteOrderUseCase @Inject constructor(
    private val repository: OrderRepository
) {
    suspend operator fun invoke(orderId: Int) =
        repository.deleteOrder(orderId)
}