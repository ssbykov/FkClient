package ru.faserkraft.client.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import ru.faserkraft.client.dto.ProductDto


const val BASE_URL = "http://194.226.9.27:8000/api/v1/"

interface Api {
    @GET(BASE_URL + "products/{serial_number}")
    suspend fun getProduct(
        @Path("serial_number") id: String,
    ): Response<ProductDto>
}