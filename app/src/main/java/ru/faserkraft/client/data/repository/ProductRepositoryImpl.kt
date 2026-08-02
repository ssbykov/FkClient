package ru.faserkraft.client.data.repository


import ru.faserkraft.client.data.dto.ProductCreateDto
import ru.faserkraft.client.data.mapper.toDomain
import ru.faserkraft.client.data.mapper.toDto
import ru.faserkraft.client.data.network.Api
import ru.faserkraft.client.domain.model.PeriodStatistics
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.ProductShort
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.model.ProductsOverview
import ru.faserkraft.client.domain.repository.ProductRepository
import ru.faserkraft.client.error.AppError
import ru.faserkraft.client.utils.logger.Logger
import ru.faserkraft.client.utils.timeprovider.TimeProvider
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val api: Api,
    private val timeProvider: TimeProvider,
    logger: Logger,
) : BaseRepository(logger), ProductRepository {

    override suspend fun getProduct(serialNumber: String): Product? =
        callApi { api.getProduct(serialNumber) }?.toDomain()

    override suspend fun getProductsByStatus(statuses: List<ProductStatus>): List<Product> {
        return callApi { api.getProductsNotNormal() }
            .orEmpty()
            .map { it.toDomain() }
            .filter { it.status in statuses }
    }

    override suspend fun createProduct(serialNumber: String, processId: Int): Product =
        requireNotNull(
            callApi {
                api.postProduct(
                    ProductCreateDto(
                        processId = processId,
                        serialNumber = serialNumber,
                        createdAt = timeProvider.nowIsoUtc()
                    )
                )
            }
        ).toDomain()

    override suspend fun changeStatus(productId: Long, status: ProductStatus): Product =
        requireNotNull(
            callApi { api.changeProductStatus(productId, status.toDto()) }
        ).toDomain()

    override suspend fun changeProcess(productId: Long, newProcessId: Int): Product =
        requireNotNull(callApi { api.changeProductProcess(productId, newProcessId) }).toDomain()

    override suspend fun getProductsOverview(): List<ProductsOverview> =
        callApi { api.getProductsOverview() }.orEmpty().map { it.toDomain() }

    override suspend fun getFinishedProducts(): List<ProductShort> =
        callApi { api.getFinishedProduct() }.orEmpty().map { it.toDomain() }

    override suspend fun getFinishedProductsByPeriod(
        dateFrom: String,
        dateTo: String,
    ): PeriodStatistics =
        requireNotNull(
            callApi {
                api.getFinishedProductsByPeriod(dateFrom, dateTo)
            }
        ).toDomain()

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