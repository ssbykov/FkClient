package ru.faserkraft.client.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import ru.faserkraft.client.BuildConfig
import ru.faserkraft.client.data.dto.*

/**
 * API интерфейс для работы с товарами
 */
interface ProductApi {

    @GET(BuildConfig.BASE_URL + "products/by-serial/{serial_number}")
    suspend fun getProduct(@Path("serial_number") serialNumber: String): Response<ProductDto>

    @POST(BuildConfig.BASE_URL + "products")
    suspend fun createProduct(@Body product: ProductCreateDto): Response<ProductDto>

    @PUT(BuildConfig.BASE_URL + "products/{id}/status")
    suspend fun updateProductStatus(
        @Path("id") productId: Long,
        @Body status: Map<String, String>
    ): Response<ProductDto>

    @PUT(BuildConfig.BASE_URL + "products/{id}/process")
    suspend fun changeProductProcess(
        @Path("id") productId: Long,
        @Body process: Map<String, Int>
    ): Response<ProductDto>

    @POST(BuildConfig.BASE_URL + "steps/{id}/complete")
    suspend fun completeStep(@Path("id") stepId: Int): Response<ProductDto>

    @GET(BuildConfig.BASE_URL + "products/by-last-completed-step")
    suspend fun getProductsByLastCompletedStep(
        @Query("process_id") processId: Int,
        @Query("step_definition_id") stepDefinitionId: Int
    ): Response<List<ProductDto>>

    @GET(BuildConfig.BASE_URL + "products/finished")
    suspend fun getFinishedProducts(): Response<List<ProductDto>>
}
