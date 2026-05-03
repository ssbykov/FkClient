package ru.faserkraft.client.data.repository

import ru.faserkraft.client.data.mapper.toDomain
import ru.faserkraft.client.domain.model.Packaging
import ru.faserkraft.client.domain.repository.PackagingRepository
import ru.faserkraft.client.dto.PackagingCreateDto
import ru.faserkraft.client.error.AppError
import ru.faserkraft.client.repository.ApiRepository
import javax.inject.Inject

class PackagingRepositoryImpl @Inject constructor(
    private val apiRepository: ApiRepository,
) : PackagingRepository {

    override suspend fun getPackaging(serialNumber: String): Packaging? {
        return try {
            apiRepository.getPackaging(serialNumber)?.toDomain()
        } catch (e: AppError.ApiError) {
            if (e.status == 404) null
            else throw e
        }
    }

    override suspend fun createPackaging(serialNumber: String, productIds: List<Int>): Packaging =
        requireNotNull(
            apiRepository.createPackaging(
                PackagingCreateDto(
                    serialNumber = serialNumber,
                    products = productIds,
                )
            )
        ).toDomain()

    override suspend fun deletePackaging(serialNumber: String) {
        apiRepository.deletePackaging(serialNumber)
    }

    override suspend fun getPackagingInStorage(): List<Packaging> =
        apiRepository.getPackagingInStorage().orEmpty().map { it.toDomain() }
}