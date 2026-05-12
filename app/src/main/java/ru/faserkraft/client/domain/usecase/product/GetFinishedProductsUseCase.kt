package ru.faserkraft.client.domain.usecase.product

import ru.faserkraft.client.domain.model.ProductShort
import ru.faserkraft.client.domain.repository.ProductRepository
import javax.inject.Inject

class GetFinishedProductsUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(): List<ProductShort> =
        repository.getFinishedProducts()
}