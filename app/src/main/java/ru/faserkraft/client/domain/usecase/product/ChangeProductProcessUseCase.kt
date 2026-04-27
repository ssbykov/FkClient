package ru.faserkraft.client.domain.usecase.product

import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.repository.ProductRepository
import javax.inject.Inject

/**
 * Use Case для изменения процесса товара
 */
class ChangeProductProcessUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(productId: Long, newProcessId: Int): Result<Product> {
        return productRepository.changeProductProcess(productId, newProcessId)
    }
}

