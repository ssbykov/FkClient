package ru.faserkraft.client.data.repository

import ru.faserkraft.client.api.AuthApi
import ru.faserkraft.client.data.callApi
import ru.faserkraft.client.data.mapper.toDto
import ru.faserkraft.client.domain.model.LoginCredentials
import ru.faserkraft.client.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
) : AuthRepository {

    override suspend fun login(credentials: LoginCredentials): String =
        requireNotNull(
            callApi { authApi.login(credentials.toDto()) }
        ) { "Login response is null" }.accessToken
}