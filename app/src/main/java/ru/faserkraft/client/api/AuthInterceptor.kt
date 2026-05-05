package ru.faserkraft.client.api

import okhttp3.Interceptor
import okhttp3.Response
import ru.faserkraft.client.auth.AppAuth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val appAuth: AppAuth
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = appAuth.getToken()

        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        return chain.proceed(request)
    }
}