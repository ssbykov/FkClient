package ru.faserkraft.client.api

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Route
import ru.faserkraft.client.auth.AppAuth
import javax.inject.Inject

class TokenAuthenticator @Inject constructor(
    private val api: AuthApi,
    private val appAuth: AppAuth
) : Authenticator {
    override fun authenticate(route: Route?, response: okhttp3.Response): Request? {
        // если уже пробовали этот же запрос несколько раз — не зацикливаемся
        if (responseCount(response) >= 2) {
            return null
        }

        val loginData = appAuth.getLoginData() ?: return null

        // синхронный запрос за новым токеном
        val loginResponse = api.loginSync(loginData).execute()
        if (!loginResponse.isSuccessful) return null

        val loginDto = loginResponse.body() ?: return null
        val newToken = loginDto.accessToken
        appAuth.saveToken(newToken)

        // новый запрос с НОВЫМ токеном
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }

    private fun responseCount(response: okhttp3.Response): Int {
        var result = 1
        var prev = response.priorResponse
        while (prev != null) {
            result++
            prev = prev.priorResponse
        }
        return result
    }
}