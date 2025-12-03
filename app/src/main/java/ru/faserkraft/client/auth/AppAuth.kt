package ru.faserkraft.client.auth

import ru.faserkraft.client.dto.LoginData

interface AppAuth {
    fun setLoginData(email: String, password: String)
    fun getLoginData(): LoginData?
    fun saveToken(token: String)
    fun getToken(): String?
    fun checkRegistration(): String?
}