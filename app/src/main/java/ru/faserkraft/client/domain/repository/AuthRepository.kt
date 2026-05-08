package ru.faserkraft.client.domain.repository

import ru.faserkraft.client.domain.model.LoginCredentials

interface AuthRepository {
    suspend fun login(credentials: LoginCredentials): String
}