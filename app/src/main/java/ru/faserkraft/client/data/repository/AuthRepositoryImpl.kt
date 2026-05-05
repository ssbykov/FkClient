package ru.faserkraft.client.data.repository

import ru.faserkraft.client.api.AuthApi
import ru.faserkraft.client.data.callApi
import ru.faserkraft.client.domain.repository.AuthRepository
import ru.faserkraft.client.dto.LoginData
import ru.faserkraft.client.dto.LoginDto
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
) : AuthRepository {

    override suspend fun login(loginData: LoginData): LoginDto {
        return requireNotNull(
            callApi { authApi.login(loginData) }
        ) { "Login response is null" }
    }
}