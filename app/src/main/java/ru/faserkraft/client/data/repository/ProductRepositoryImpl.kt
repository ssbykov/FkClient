package ru.faserkraft.client.data.repository

import ru.faserkraft.client.data.api.ProductApi
import ru.faserkraft.client.data.dto.ProductCreateDto
import ru.faserkraft.client.data.dto.toDomain
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.repository.ProductRepository
import ru.faserkraft.client.error.AppError
import ru.faserkraft.client.utils.callApi
import javax.inject.Inject

/**
 * Реализация ProductRepository
 */
class ProductRepositoryImpl @Inject constructor(
    private val productApi: ProductApi
) : ProductRepository {

    override suspend fun getProductBySerialNumber(serialNumber: String): Result<Product> {
        return try {
            val response = productApi.getProduct(serialNumber)
            callApi(response).map { it.toDomain() }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }

    override suspend fun getProductById(id: Long): Result<Product> {
        // TODO: Реализовать через API если есть endpoint
        return Result.failure(AppError.UnknownError)
    }

    override suspend fun createProduct(
        serialNumber: String,
        processId: Int
    ): Result<Product> {
        return try {
            val request = ProductCreateDto(serialNumber, processId)
            val response = productApi.createProduct(request)
            callApi(response).map { it.toDomain() }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }

    override suspend fun updateProductStatus(
        productId: Long,
        status: String
    ): Result<Product> {
        return try {
            val request = mapOf("status" to status)
            val response = productApi.updateProductStatus(productId, request)
            callApi(response).map { it.toDomain() }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }

    override suspend fun changeProductProcess(
        productId: Long,
        newProcessId: Int
    ): Result<Product> {
        return try {
            val request = mapOf("process_id" to newProcessId)
            val response = productApi.changeProductProcess(productId, request)
            callApi(response).map { it.toDomain() }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }

    override suspend fun completeStep(stepId: Int): Result<Product> {
        return try {
            val response = productApi.completeStep(stepId)
            callApi(response).map { it.toDomain() }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }

    override suspend fun getProductsByLastCompletedStep(
        processId: Int,
        stepDefinitionId: Int
    ): Result<List<Product>> {
        return try {
            val response = productApi.getProductsByLastCompletedStep(processId, stepDefinitionId)
            callApi(response).map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }

    override suspend fun getFinishedProducts(): Result<List<Product>> {
        return try {
            val response = productApi.getFinishedProducts()
            callApi(response).map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }
}