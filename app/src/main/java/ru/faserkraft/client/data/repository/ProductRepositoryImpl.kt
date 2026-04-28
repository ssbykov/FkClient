package ru.faserkraft.client.data.repository

import ru.faserkraft.client.data.mapper.toDomain
import ru.faserkraft.client.data.mapper.toDto
import ru.faserkraft.client.domain.model.FinishedProduct
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.model.ProductsInventory
import ru.faserkraft.client.domain.repository.ProductRepository
import ru.faserkraft.client.repository.ApiRepository
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val apiRepository: ApiRepository,
) : ProductRepository {

    override suspend fun getProduct(serialNumber: String): Product? =
        apiRepository.getProduct(serialNumber)?.toDomain()

    override suspend fun createProduct(serialNumber: String, processId: Int): Product =
        requireNotNull(
            apiRepository.postProduct(
                ru.faserkraft.client.dto.ProductCreateDto(
                    processId = processId,
                    serialNumber = serialNumber,
                )
            )
        ).toDomain()

    override suspend fun changeStatus(productId: Long, status: ProductStatus): Product =
        requireNotNull(apiRepository.changeProductStatus(productId, status.toDto())).toDomain()

    override suspend fun changeProcess(productId: Long, newProcessId: Int): Product =
        requireNotNull(apiRepository.changeProductProcess(productId, newProcessId)).toDomain()

    override suspend fun getProductsInventory(): List<ProductsInventory> =
        apiRepository.getProductsInventory().orEmpty().map { it.toDomain() }

    override suspend fun getFinishedProducts(): List<FinishedProduct> =
        apiRepository.getFinishedProduct().orEmpty().map { it.toDomain() }

    override suspend fun getProductsByLastCompletedStep(
        processId: Int,
        stepDefinitionId: Int,
    ): List<Product> =
        apiRepository.getProductsByLastCompletedStep(processId, stepDefinitionId)
            .orEmpty()
            .map { it.toDomain() }

    override suspend fun getProductsByStepEmployeeDay(
        stepDefinitionId: Int,
        day: String,
        employeeId: Int,
    ): List<Product> =
        apiRepository.getProductsByStepEmployeeDay(stepDefinitionId, day, employeeId)
            .orEmpty()
            .map { it.toDomain() }
}