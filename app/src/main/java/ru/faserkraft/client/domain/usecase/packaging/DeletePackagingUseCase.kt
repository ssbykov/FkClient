package ru.faserkraft.client.domain.usecase.packaging

import ru.faserkraft.client.domain.repository.PackagingRepository
import javax.inject.Inject

class DeletePackagingUseCase @Inject constructor(
    private val repository: PackagingRepository
) {
    suspend operator fun invoke(serialNumber: String) =
        repository.deletePackaging(serialNumber)
}