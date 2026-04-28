package ru.faserkraft.client.domain.usecase.product

import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.repository.ProductRepository
import javax.inject.Inject

class GetProductUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(serialNumber: String): Product? =
        repository.getProduct(serialNumber)
}