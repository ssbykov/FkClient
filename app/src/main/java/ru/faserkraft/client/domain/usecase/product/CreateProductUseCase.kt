package ru.faserkraft.client.domain.usecase.product

import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.repository.ProductRepository
import javax.inject.Inject

/**
 * Use Case для создания товара
 */
class CreateProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(
        serialNumber: String,
        processId: Int
    ): Result<Product> {
        return productRepository.createProduct(serialNumber, processId)
    }
}

