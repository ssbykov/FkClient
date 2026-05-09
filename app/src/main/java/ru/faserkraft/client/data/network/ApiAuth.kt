package ru.faserkraft.client.data.network

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import ru.faserkraft.client.data.dto.DeviceRequestDto
import ru.faserkraft.client.data.dto.DeviceResponseDto
import ru.faserkraft.client.data.dto.LoginRequestDto
import ru.faserkraft.client.data.dto.LoginDto

interface AuthApi {

    @POST(BASE_URL + "auth/login_json")
    fun loginSync(@Body loginRequestDto: LoginRequestDto): Call<LoginDto>

    @POST(BASE_URL + "auth/login_json")
    suspend fun login(@Body loginRequestDto: LoginRequestDto): Response<LoginDto>

    @POST(BASE_URL + "users/new-device")
    suspend fun registerDevice(
        @Body request: DeviceRequestDto
    ): Response<DeviceResponseDto>
}