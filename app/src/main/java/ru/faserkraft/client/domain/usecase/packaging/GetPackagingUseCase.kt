package ru.faserkraft.client.domain.usecase.packaging

import ru.faserkraft.client.domain.model.Packaging
import ru.faserkraft.client.domain.repository.PackagingRepository
import javax.inject.Inject

class GetPackagingUseCase @Inject constructor(
    private val packagingRepository: PackagingRepository
) {
    suspend operator fun invoke(serialNumber: String): Result<Packaging> {
        return packagingRepository.getPackagingBySerialNumber(serialNumber)
    }
}

