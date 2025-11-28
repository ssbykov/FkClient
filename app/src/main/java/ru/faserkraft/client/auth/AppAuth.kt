package ru.faserkraft.client.auth

interface AppAuth {
    fun setLoginData(email: String, password: String)
    fun saveToken(token: String)
    fun getToken(): String?
}