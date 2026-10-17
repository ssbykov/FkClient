package ru.faserkraft.client.auth

import ru.faserkraft.client.domain.model.UserData

interface AppAuth {
    fun saveSession(token: String, expiresAt: String, userData: UserData)
    fun getRegistrationData(): UserData?
    fun getToken(): String?
    fun checkRegistration(): String?
    fun clear()
}