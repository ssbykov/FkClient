package ru.faserkraft.client.domain.repository

import ru.faserkraft.client.dto.LoginData
import ru.faserkraft.client.dto.LoginDto

interface AuthRepository {
    suspend fun login(loginData: LoginData): LoginDto
}