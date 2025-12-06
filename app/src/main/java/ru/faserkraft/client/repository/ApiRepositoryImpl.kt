package ru.faserkraft.client.repository

import ru.faserkraft.client.api.Api
import ru.faserkraft.client.dto.DeviceRequestDto
import ru.faserkraft.client.dto.DeviceResponseDto
import ru.faserkraft.client.dto.ProcessDto
import ru.faserkraft.client.dto.ProductCreateDto
import ru.faserkraft.client.dto.ProductDto
import ru.faserkraft.client.dto.StepCloseDto
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

    override suspend fun postStep(step: StepCloseDto): ProductDto? =
        callApi { api.postStep(step) }
}
