package ru.faserkraft.client.domain.usecase.product

import ru.faserkraft.client.domain.model.ProductsInventory
import ru.faserkraft.client.domain.repository.ProductRepository
import javax.inject.Inject

class GetProductsInventoryUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(): List<ProductsInventory> =
        repository.getProductsInventory()
}