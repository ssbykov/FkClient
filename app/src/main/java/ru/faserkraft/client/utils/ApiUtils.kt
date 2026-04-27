package ru.faserkraft.client.utils

import retrofit2.Response
import ru.faserkraft.client.error.AppError

/**
 * Утилита для обработки API ответов
 */
suspend fun <T> callApi(response: Response<T>): Result<T> {
    return if (response.isSuccessful) {
        val body = response.body()
        if (body != null) {
            Result.success(body)
        } else {
            Result.failure(AppError.UnknownError)
        }
    } else {
        val error = AppError.ApiError(response.code(), "api_error", response.message())
        Result.failure(error)
    }
}
