package ru.faserkraft.client.repository

import ru.faserkraft.client.api.Api
import ru.faserkraft.client.dto.DayPlanDto
import ru.faserkraft.client.dto.DeviceRequestDto
import ru.faserkraft.client.dto.DeviceResponseDto
import ru.faserkraft.client.dto.ProcessDto
import ru.faserkraft.client.dto.ProductCreateDto
import ru.faserkraft.client.dto.ProductDto
import javax.inject.Inject

class ApiRepositoryImpl @Inject constructor(
    private val api: Api,
) : ApiRepository {

    override suspend fun getProduct(serialNumber: String): ProductDto? =
        callApi { api.getProduct(serialNumber) }

    override suspend fun postProduct(product: ProductCreateDto): ProductDto? =
        callApi { api.postProduct(product) }

    override suspend fun getProcesses(): List<ProcessDto>? =
        callApi { api.getProcesses() }

    override suspend fun postDevice(device: DeviceRequestDto): DeviceResponseDto? =
        callApi { api.postDevice(device) }

    override suspend fun postStep(stepId: Int): ProductDto? =
        callApi { api.postStep(stepId) }

    override suspend fun getDayPlans(
        employeeId: String?,
        date: String
    ): List<DayPlanDto>? = callApi { api.getDayPlans() }

}
