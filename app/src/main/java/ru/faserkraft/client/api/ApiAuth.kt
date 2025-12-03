package ru.faserkraft.client.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import ru.faserkraft.client.dto.LoginData
import ru.faserkraft.client.dto.LoginDto

interface AuthApi {
    @POST(BASE_URL + "auth/login_json")
    fun loginSync(@Body loginData: LoginData): Call<LoginDto>
}