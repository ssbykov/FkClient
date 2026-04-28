package ru.faserkraft.client.domain.usecase.packaging

import ru.faserkraft.client.domain.model.Packaging
import ru.faserkraft.client.domain.repository.PackagingRepository
import javax.inject.Inject

class CreatePackagingUseCase @Inject constructor(
    private val repository: PackagingRepository
) {
    suspend operator fun invoke(serialNumber: String, productIds: List<Int>): Packaging =
        repository.createPackaging(serialNumber, productIds)
}