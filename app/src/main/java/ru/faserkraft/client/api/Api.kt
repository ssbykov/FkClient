package ru.faserkraft.client.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import ru.faserkraft.client.dto.DayPlanDto
import ru.faserkraft.client.dto.DeviceRequestDto
import ru.faserkraft.client.dto.DeviceResponseDto
import ru.faserkraft.client.dto.EmployeeDto
import ru.faserkraft.client.dto.ProcessDto
import ru.faserkraft.client.dto.ProductCreateDto
import ru.faserkraft.client.dto.ProductDto


const val BASE_URL = "https://product.faserkraft.ru/api/v1/"

interface Api {
    @GET(BASE_URL + "products/{serial_number}")
    suspend fun getProduct(
        @Path("serial_number") id: String,
    ): Response<ProductDto>

    @GET(BASE_URL + "processes/")
    suspend fun getProcesses(): Response<List<ProcessDto>>

    @GET(BASE_URL + "employees/")
    suspend fun getEmployees(): Response<List<EmployeeDto>>

    @GET(BASE_URL + "daily-plans")
    suspend fun getDayPlans(
        @Query("plan_date") date: String
    ): Response<List<DayPlanDto>>

    @POST(BASE_URL + "products/")
    suspend fun postProduct(
        @Body product: ProductCreateDto
    ): Response<ProductDto>

    @POST(BASE_URL + "products_steps/")
    suspend fun postStep(
        @Query("step_id") stepId: Int
    ): Response<ProductDto>

    @POST(BASE_URL + "products/change_product_process")
    suspend fun changeProductProcess(
        @Query("product_id") productId: Long,
        @Query("new_process_id") newProcessId: Int
    ): Response<ProductDto>

    @POST(BASE_URL + "products/{product_id}/send_to_scrap")
    suspend fun sendToScrap(
        @Path("product_id") productId: Long
    ): Response<ProductDto>

    @POST(BASE_URL + "products/{product_id}/send_to_rework")
    suspend fun sendToRework(
        @Path("product_id") productId: Long
    ): Response<ProductDto>

    @POST(BASE_URL + "products/{product_id}/restore_from_scrap")
    suspend fun restoreFromScrap(
        @Path("product_id") productId: Long
    ): Response<ProductDto>

    @POST(BASE_URL + "users/new-device")
    suspend fun postDevice(
        @Body device: DeviceRequestDto
    ): Response<DeviceResponseDto>
}
