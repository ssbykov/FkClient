package ru.faserkraft.client.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import ru.faserkraft.client.dto.DeviceRequestDto
import ru.faserkraft.client.dto.DeviceResponseDto
import ru.faserkraft.client.dto.ProcessDto
import ru.faserkraft.client.dto.ProductDto
import ru.faserkraft.client.dto.StepCloseDto


const val BASE_URL = "https://product.faserkraft.ru/api/v1/"

interface Api {
    @GET(BASE_URL + "products/{serial_number}")
    suspend fun getProduct(
        @Path("serial_number") id: String,
    ): Response<ProductDto>

    @GET(BASE_URL + "processes/")
    suspend fun getProcesses(): Response<List<ProcessDto>>

    @POST(BASE_URL + "products_steps/")
    suspend fun postStep(
        @Body step: StepCloseDto
    ): Response<ProductDto>

    @POST(BASE_URL + "users/new-device")
    suspend fun postDevice(
        @Body device: DeviceRequestDto
    ): Response<DeviceResponseDto>
}
