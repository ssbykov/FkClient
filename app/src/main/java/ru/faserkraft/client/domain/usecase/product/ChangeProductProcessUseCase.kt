package ru.faserkraft.client.domain.usecase.product

import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.repository.ProductRepository
import javax.inject.Inject

class ChangeProductProcessUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(productId: Long, newProcessId: Int): Product =
        repository.changeProcess(productId, newProcessId)
}