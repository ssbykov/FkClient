package ru.faserkraft.client.auth

import ru.faserkraft.client.domain.model.UserData

interface AppAuth {
    fun saveSession(token: String, refreshToken: String, expiresAt: String, userData: UserData)
    fun updateTokens(token: String, refreshToken: String, expiresAt: String)
    fun getRegistrationData(): UserData?
    fun getToken(): String?
    fun getRefreshToken(): String?
    fun checkRegistration(): String?
    fun clear()
}