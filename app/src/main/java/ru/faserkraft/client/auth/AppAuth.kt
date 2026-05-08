package ru.faserkraft.client.auth

import ru.faserkraft.client.domain.model.UserData
import ru.faserkraft.client.dto.LoginRequestDto

interface AppAuth {
    fun saveUserData(userData: UserData)
    fun getRegistrationData(): UserData?
    fun getLoginData(): LoginRequestDto?   // для presentation/domain
    fun saveToken(token: String)
    fun getToken(): String?
    fun checkRegistration(): String?
    fun clear()
}