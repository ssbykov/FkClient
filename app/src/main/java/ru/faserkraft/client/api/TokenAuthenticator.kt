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
        // если уже пробовали авторизоваться этим запросом — выходим, чтобы не зациклиться
        if (response.request.header("Authorization") != null) {
            return null
        }

        val loginData = appAuth.getLoginData() ?: return null

        // синхронный запрос за новым токеном
        val loginResponse = api.loginSync(loginData).execute()
        if (!loginResponse.isSuccessful) {
            return null
        }

        val loginDto = loginResponse.body() ?: return null
        val newToken = loginDto.accessToken
        appAuth.saveToken(newToken)

        // возвращаем новый запрос с обновлённым токеном
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }
}