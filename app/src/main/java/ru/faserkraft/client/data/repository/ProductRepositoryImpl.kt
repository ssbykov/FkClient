package ru.faserkraft.client.data.repository

import ru.faserkraft.client.api.Api
import ru.faserkraft.client.data.callApi
import ru.faserkraft.client.data.mapper.toDomain
import ru.faserkraft.client.data.mapper.toDto
import ru.faserkraft.client.domain.model.FinishedProduct
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.model.ProductsInventory
import ru.faserkraft.client.domain.repository.ProductRepository
import ru.faserkraft.client.dto.ProductCreateDto
import ru.faserkraft.client.dto.toBackendValue
import ru.faserkraft.client.error.AppError
import ru.faserkraft.client.utils.nowIsoUtc
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val api: Api,
) : ProductRepository {

    override suspend fun getProduct(serialNumber: String): Product? =
        try {
            callApi { api.getProduct(serialNumber) }?.toDomain()
        } catch (e: AppError.ApiError) {
            if (e.status == 404) null else throw e
        }

    override suspend fun createProduct(serialNumber: String, processId: Int): Product =
        requireNotNull(
            callApi {
                api.postProduct(
                    ProductCreateDto(
                        processId = processId,
                        serialNumber = serialNumber,
                        createdAt = nowIsoUtc()
                    )
                )
            }
        ).toDomain()

    override suspend fun changeStatus(productId: Long, status: ProductStatus): Product =
        requireNotNull(
            callApi { api.changeProductStatus(productId, status.toDto().toBackendValue()) }
        ).toDomain()

    override suspend fun changeProcess(productId: Long, newProcessId: Int): Product =
        requireNotNull(callApi { api.changeProductProcess(productId, newProcessId) }).toDomain()

    override suspend fun getProductsInventory(): List<ProductsInventory> =
        callApi { api.getProductsInventory() }.orEmpty().map { it.toDomain() }

    override suspend fun getFinishedProducts(): List<FinishedProduct> =
        callApi { api.getFinishedProduct() }.orEmpty().map { it.toDomain() }

    override suspend fun getProductsByLastCompletedStep(
        processId: Int,
        stepDefinitionId: Int,
    ): List<Product> =
        callApi { api.getProductsByLastCompletedStep(processId, stepDefinitionId) }
            .orEmpty()
            .map { it.toDomain() }

    override suspend fun getProductsByStepEmployeeDay(
        stepDefinitionId: Int,
        day: String,
        employeeId: Int,
    ): List<Product> =
        callApi { api.getProductsByStepEmployeeDay(stepDefinitionId, day, employeeId) }
            .orEmpty()
            .map { it.toDomain() }
}