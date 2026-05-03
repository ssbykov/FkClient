package ru.faserkraft.client.data.repository

import ru.faserkraft.client.api.Api
import ru.faserkraft.client.data.callApi
import ru.faserkraft.client.data.callApiUnit
import ru.faserkraft.client.data.mapper.toDomain
import ru.faserkraft.client.domain.model.Packaging
import ru.faserkraft.client.domain.repository.PackagingRepository
import ru.faserkraft.client.dto.PackagingCreateDto
import ru.faserkraft.client.error.AppError
import javax.inject.Inject

class PackagingRepositoryImpl @Inject constructor(
    private val api: Api,
) : PackagingRepository {

    override suspend fun getPackaging(serialNumber: String): Packaging? =
        try {
            callApi { api.getPackaging(serialNumber) }?.toDomain()
        } catch (e: AppError.ApiError) {
            if (e.status == 404) null else throw e
        }

    override suspend fun createPackaging(serialNumber: String, productIds: List<Int>): Packaging =
        requireNotNull(
            callApi {
                api.createPackaging(
                    PackagingCreateDto(
                        serialNumber = serialNumber,
                        products = productIds,
                    )
                )
            }
        ).toDomain()

    override suspend fun deletePackaging(serialNumber: String) =
        callApiUnit { api.deletePackaging(serialNumber) }

    override suspend fun getPackagingInStorage(): List<Packaging> =
        callApi { api.getPackagingInStorage() }.orEmpty().map { it.toDomain() }
}