package ru.faserkraft.client.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import ru.faserkraft.client.BuildConfig
import ru.faserkraft.client.data.dto.FinishedProductDto
import ru.faserkraft.client.data.dto.PackagingDto

/**
 * API интерфейс для работы с упаковкой
 */
interface PackagingApi {

    @GET(BuildConfig.BASE_URL + "packaging/by-serial/{serial_number}")
    suspend fun getPackaging(@Path("serial_number") serialNumber: String): Response<PackagingDto>

    @POST(BuildConfig.BASE_URL + "packaging")
    suspend fun createPackaging(@Body request: Map<String, Any>): Response<PackagingDto>

    @GET(BuildConfig.BASE_URL + "packaging/storage")
    suspend fun getPackagingInStorage(): Response<List<PackagingDto>>

    @DELETE(BuildConfig.BASE_URL + "packaging/{serial_number}")
    suspend fun deletePackaging(@Path("serial_number") serialNumber: String): Response<Unit>

    @GET(BuildConfig.BASE_URL + "products/finished")
    suspend fun getFinishedProducts(): Response<List<FinishedProductDto>>
}
