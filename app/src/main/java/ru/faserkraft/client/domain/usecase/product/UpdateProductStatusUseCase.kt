package ru.faserkraft.client.domain.usecase.product

import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.repository.ProductRepository
import javax.inject.Inject

/**
 * Use Case для обновления статуса товара
 */
class UpdateProductStatusUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(productId: Long, status: String): Result<Product> {
        return productRepository.updateProductStatus(productId, status)
    }
}

