package ru.faserkraft.client.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import ru.faserkraft.client.data.dto.DeviceRequestDto
import ru.faserkraft.client.data.dto.DeviceResponseDto
import ru.faserkraft.client.data.dto.RefreshRequestDto
import ru.faserkraft.client.data.dto.RefreshResponseDto

interface AuthApi {

    @POST(BASE_URL + "mobile/register-device")
    suspend fun registerDevice(
        @Body request: DeviceRequestDto,
    ): Response<DeviceResponseDto>

    @POST(BASE_URL + "mobile/refresh")
    suspend fun refresh(
        @Body request: RefreshRequestDto,
    ): Response<RefreshResponseDto>
}