package ru.faserkraft.client.domain.usecase.order

import ru.faserkraft.client.domain.repository.OrderRepository
import javax.inject.Inject

class DetachPackagingFromOrderUseCase @Inject constructor(
    private val repository: OrderRepository
) {
    suspend operator fun invoke(packagingIds: List<Int>) =
        repository.detachPackagingFromOrder(packagingIds)
}