package ru.faserkraft.client.repository

import ru.faserkraft.client.api.Api
import ru.faserkraft.client.dto.DailyPlanStepCreateDto
import ru.faserkraft.client.dto.DailyPlanStepUpdateDto
import ru.faserkraft.client.dto.DayPlanDto
import ru.faserkraft.client.dto.DeviceRequestDto
import ru.faserkraft.client.dto.DeviceResponseDto
import ru.faserkraft.client.dto.EmployeeDto
import ru.faserkraft.client.dto.FinishedProductDto
import ru.faserkraft.client.dto.PackagingCreateDto
import ru.faserkraft.client.dto.PackagingDto
import ru.faserkraft.client.dto.ProcessDto
import ru.faserkraft.client.dto.ProductCreateDto
import ru.faserkraft.client.dto.ProductDto
import ru.faserkraft.client.dto.ProductStatus
import ru.faserkraft.client.dto.ProductsInventoryDto
import ru.faserkraft.client.dto.QrDataResponseDto
import ru.faserkraft.client.dto.toBackendValue
import javax.inject.Inject

class ApiRepositoryImpl @Inject constructor(
    private val api: Api,
) : ApiRepository {

    override suspend fun getProduct(serialNumber: String): ProductDto? =
        callApi { api.getProduct(serialNumber) }

    override suspend fun getPackaging(serialNumber: String): PackagingDto? =
        callApi { api.getPackaging(serialNumber) }

    override suspend fun postProduct(product: ProductCreateDto): ProductDto? =
        callApi { api.postProduct(product) }

    override suspend fun createPackaging(packaging: PackagingCreateDto): PackagingDto? =
        callApi { api.createPackaging(packaging) }

    override suspend fun getProcesses(): List<ProcessDto>? =
        callApi { api.getProcesses() }

    override suspend fun getEmployees(): List<EmployeeDto>? =
        callApi { api.getEmployees() }

    override suspend fun postDevice(device: DeviceRequestDto): DeviceResponseDto? =
        callApi { api.postDevice(device) }

    override suspend fun getQrCode(employeeId: Int): QrDataResponseDto? =
        callApi { api.getQrCode(employeeId) }

    override suspend fun postStep(stepId: Int): ProductDto? =
        callApi { api.postStep(stepId) }

    override suspend fun changeProductProcess(
        productId: Long,
        newProcessId: Int
    ): ProductDto? = callApi { api.changeProductProcess(productId, newProcessId) }

    override suspend fun getProductsInventory(
    ): List<ProductsInventoryDto>? = callApi { api.getProductsInventory() }

    override suspend fun getProductsByLastCompletedStep(
        processId: Int,
        stepDefinitionId: Int
    ): List<ProductDto>? =
        callApi { api.getProductsByLastCompletedStep(processId, stepDefinitionId) }

    override suspend fun getFinishedProduct(
    ): List<FinishedProductDto>? = callApi { api.getFinishedProduct() }

    override suspend fun getDayPlans(
        date: String
    ): List<DayPlanDto>? = callApi { api.getDayPlans(date) }

    override suspend fun changeProductStatus(
        productId: Long,
        status: ProductStatus,
    ): ProductDto? =
        callApi { api.changeProductStatus(productId, status.toBackendValue()) }

    override suspend fun changeStepPerformer(
        stepId: Int,
        newEmployeeId: Int,
    ): ProductDto? = callApi { api.changeStepPerformer(stepId, newEmployeeId) }

    override suspend fun addStepToDailyPlan(
        body: DailyPlanStepCreateDto
    ): List<DayPlanDto>? = callApi { api.addStepToDailyPlan(body) }

    override suspend fun removeStepFromDailyPlan(
        dailyPlanStepId: Int
    ): List<DayPlanDto>? = callApi { api.removeStepFromDailyPlan(dailyPlanStepId) }

    override suspend fun updateStepInDailyPlan(
        body: DailyPlanStepUpdateDto
    ): List<DayPlanDto>? = callApi { api.updateStepInDailyPlan(body) }

}
