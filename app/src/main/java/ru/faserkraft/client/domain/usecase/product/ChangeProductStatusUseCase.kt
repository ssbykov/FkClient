package ru.faserkraft.client.domain.usecase.product

import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.repository.ProductRepository
import javax.inject.Inject

class ChangeProductStatusUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(productId: Long, status: ProductStatus): Product =
        repository.changeStatus(productId, status)
}