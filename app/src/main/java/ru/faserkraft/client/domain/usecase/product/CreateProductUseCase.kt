package ru.faserkraft.client.domain.usecase.product

import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.repository.ProductRepository
import javax.inject.Inject

class CreateProductUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(serialNumber: String, processId: Int): Product =
        repository.createProduct(serialNumber, processId)
}