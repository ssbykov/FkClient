package ru.faserkraft.client.domain.usecase.product

import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.repository.ProductRepository
import javax.inject.Inject

/**
 * Use Case для завершения шага товара
 */
class CompleteStepUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(stepId: Int): Result<Product> {
        return productRepository.completeStep(stepId)
    }
}

