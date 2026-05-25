package ru.faserkraft.client.domain.usecase.product

import ru.faserkraft.client.domain.model.ProductsOverview
import ru.faserkraft.client.domain.repository.ProductRepository
import javax.inject.Inject

class GetProductsOverviewUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(): List<ProductsOverview> =
        repository.getProductsOverview()
}