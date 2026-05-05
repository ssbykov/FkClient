package ru.faserkraft.client.auth

import ru.faserkraft.client.domain.model.UserData
import ru.faserkraft.client.dto.LoginData

interface AppAuth {
    fun saveUserData(userData: UserData)
    fun getLoginData(): LoginData?
    fun getRegistrationData(): UserData?
    fun saveToken(token: String)
    fun getToken(): String?
    fun checkRegistration(): String?
    fun clear()
}