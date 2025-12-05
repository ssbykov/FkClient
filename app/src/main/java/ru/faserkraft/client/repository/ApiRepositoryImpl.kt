package ru.faserkraft.client.repository

import ru.faserkraft.client.api.Api
import ru.faserkraft.client.dto.DeviceRequestDto
import ru.faserkraft.client.dto.DeviceResponseDto
import ru.faserkraft.client.dto.ProductDto
import ru.faserkraft.client.dto.StepCloseDto
import javax.inject.Inject

class ApiRepositoryImpl @Inject constructor(
    private val api: Api,
) : ApiRepository {

    private val baseRequest = BaseRequest()

    override suspend fun getProduct(serialNumber: String): ProductDto {
        return baseRequest.get(serialNumber, api::getProduct)
    }

    override suspend fun postDevice(device: DeviceRequestDto): DeviceResponseDto? {
        return baseRequest.post(api::postDevice, device)
    }

    override suspend fun postStep(step: StepCloseDto): ProductDto? {
        return baseRequest.post(api::postStep, step)
    }
}