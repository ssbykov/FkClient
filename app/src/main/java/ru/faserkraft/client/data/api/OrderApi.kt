package ru.faserkraft.client.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import ru.faserkraft.client.BuildConfig
import ru.faserkraft.client.data.dto.*

/**
 * API интерфейс для работы с заказами
 */
interface OrderApi {

    @GET(BuildConfig.BASE_URL + "orders")
    suspend fun getAllOrders(): Response<List<OrderDto>>

    @GET(BuildConfig.BASE_URL + "orders/{id}")
    suspend fun getOrder(@Path("id") orderId: Int): Response<OrderDto>

    @POST(BuildConfig.BASE_URL + "orders")
    suspend fun createOrder(@Body order: OrderCreateDto): Response<OrderDto>

    @PUT(BuildConfig.BASE_URL + "orders/{id}")
    suspend fun updateOrder(
        @Path("id") orderId: Int,
        @Body order: OrderUpdateDto
    ): Response<OrderDto>

    @POST(BuildConfig.BASE_URL + "orders/{id}/close")
    suspend fun closeOrder(@Path("id") orderId: Int): Response<OrderDto>

    @DELETE(BuildConfig.BASE_URL + "orders/{id}")
    suspend fun deleteOrder(@Path("id") orderId: Int): Response<Unit>

    @POST(BuildConfig.BASE_URL + "orders/{id}/packaging")
    suspend fun addPackagingToOrder(
        @Path("id") orderId: Int,
        @Body packagingIds: Map<String, List<Int>>
    ): Response<Unit>

    @DELETE(BuildConfig.BASE_URL + "packaging/detach")
    suspend fun detachPackagingFromOrder(@Body packagingIds: Map<String, List<Int>>): Response<Unit>
}

