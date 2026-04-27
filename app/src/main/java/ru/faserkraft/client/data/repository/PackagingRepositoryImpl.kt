package ru.faserkraft.client.data.repository

import ru.faserkraft.client.data.api.PackagingApi
import ru.faserkraft.client.data.dto.FinishedProductDto
import ru.faserkraft.client.data.dto.toDomain
import ru.faserkraft.client.domain.model.FinishedProduct
import ru.faserkraft.client.domain.model.Packaging
import ru.faserkraft.client.domain.repository.PackagingRepository
import ru.faserkraft.client.error.AppError
import ru.faserkraft.client.utils.callApi
import javax.inject.Inject

/**
 * Реализация PackagingRepository
 */
class PackagingRepositoryImpl @Inject constructor(
    private val packagingApi: PackagingApi
) : PackagingRepository {

    override suspend fun getPackagingBySerialNumber(serialNumber: String): Result<Packaging> {
        return try {
            val response = packagingApi.getPackaging(serialNumber)
            callApi(response).map { it.toDomain() }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }

    override suspend fun createPackaging(
        serialNumber: String,
        productIds: List<Long>
    ): Result<Packaging> {
        return try {
            val request = mapOf(
                "serial_number" to serialNumber,
                "product_ids" to productIds
            )
            val response = packagingApi.createPackaging(request)
            callApi(response).map { it.toDomain() }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }

    override suspend fun getPackagingInStorage(): Result<List<Packaging>> {
        return try {
            val response = packagingApi.getPackagingInStorage()
            callApi(response).map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }

    override suspend fun deletePackaging(serialNumber: String): Result<Unit> {
        return try {
            val response = packagingApi.deletePackaging(serialNumber)
            callApi(response).map { Unit }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }

    override suspend fun getFinishedProducts(): Result<List<FinishedProduct>> {
        return try {
            val response = packagingApi.getFinishedProducts()
            callApi(response).map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }
}
