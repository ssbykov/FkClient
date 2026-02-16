package ru.faserkraft.client.auth

import ru.faserkraft.client.dto.LoginData
import ru.faserkraft.client.model.UserData

interface AppAuth {
    fun saveUserData(userData: UserData)
    fun getLoginData(): LoginData?
    fun getRegistrationData(): UserData?
    fun saveToken(token: String)
    fun getToken(): String?
    fun checkRegistration(): String?
    fun resetRegistration()
}