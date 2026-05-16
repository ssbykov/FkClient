package ru.faserkraft.client.domain.usecase.product

import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.repository.ProductRepository
import javax.inject.Inject

class GetProductsByStatusUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(statuses: List<ProductStatus>): List<Product> =
        repository.getProductsByStatus(statuses)
}