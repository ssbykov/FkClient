package ru.faserkraft.client.auth

import ru.faserkraft.client.dto.LoginData
import ru.faserkraft.client.model.RegistrationModel

interface AppAuth {
    fun saveUserData(
        email: String,
        password: String,
        userName: String
        )
    fun getLoginData(): LoginData?
    fun getRegistrationData(): RegistrationModel?
    fun saveToken(token: String)
    fun getToken(): String?
    fun checkRegistration(): String?
    fun resetRegistration()
}