package ru.faserkraft.client.api

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import ru.faserkraft.client.auth.AppAuth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val authApi: AuthApi,
    private val appAuth: AppAuth
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null

        val currentToken = appAuth.getToken()
        val requestToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()

        if (!currentToken.isNullOrBlank() && currentToken != requestToken) {
            return response.request.newBuilder()
                .header("Authorization", "Bearer $currentToken")
                .build()
        }

        val loginData = appAuth.getLoginData() ?: return null

        val loginResponse = try {
            authApi.loginSync(loginData).execute()
        } catch (_: Exception) {
            return null
        }

        if (!loginResponse.isSuccessful) return null

        val newToken = loginResponse.body()?.accessToken ?: return null
        appAuth.saveToken(newToken)

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prev = response.priorResponse
        while (prev != null) {
            result++
            prev = prev.priorResponse
        }
        return result
    }
}