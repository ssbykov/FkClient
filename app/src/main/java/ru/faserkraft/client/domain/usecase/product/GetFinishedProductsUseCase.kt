package ru.faserkraft.client.domain.usecase.product

import ru.faserkraft.client.domain.model.FinishedProduct
import ru.faserkraft.client.domain.repository.PackagingRepository
import javax.inject.Inject

class GetFinishedProductsUseCase @Inject constructor(
    private val packagingRepository: PackagingRepository
) {
    suspend operator fun invoke(): Result<List<FinishedProduct>> {
        return packagingRepository.getFinishedProducts()
    }
}

