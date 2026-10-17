package ru.faserkraft.client.data.network

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import ru.faserkraft.client.auth.AppAuth
import ru.faserkraft.client.presentation.app.AppSessionCoordinator
import ru.faserkraft.client.presentation.app.AppSessionEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val appAuth: AppAuth,
    private val sessionCoordinator: AppSessionCoordinator,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? =
        synchronized(this) { authenticateLocked(response) }

    private fun authenticateLocked(response: Response): Request? {
        val requestToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()
        if (requestToken.isNullOrBlank()) return null

        val currentToken = appAuth.getToken()
        if (currentToken.isNullOrBlank()) return null

        if (currentToken != requestToken) {
            return response.request.newBuilder()
                .header("Authorization", "Bearer $currentToken")
                .build()
        }

        if (!isInvalidMobileSession(response)) return null

        appAuth.clear()
        runBlocking { sessionCoordinator.send(AppSessionEvent.Logout) }
        return null
    }

    private fun isInvalidMobileSession(response: Response): Boolean =
        runCatching {
            response.peekBody(2048).string().contains("INVALID_MOBILE_SESSION")
        }.getOrDefault(false)
}