package ru.faserkraft.client.repository

import ru.faserkraft.client.api.Api
import ru.faserkraft.client.dto.ProductDto
import javax.inject.Inject

class ApiRepositoryImpl @Inject constructor (
    private val api: Api,
) : ApiRepository {

    private val baseRequest = BaseRequest()

    override suspend fun getProduct(serialNumber: String): ProductDto {
        return baseRequest.get(serialNumber, api::getProduct)
    }
}