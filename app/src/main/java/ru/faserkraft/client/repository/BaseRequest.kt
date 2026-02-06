package ru.faserkraft.client.repository

import retrofit2.Response
import ru.faserkraft.client.error.AppError
import java.io.IOException


suspend fun <R> callApi(
    block: suspend () -> Response<R>
): R? {
    return try {
        val response = block()

        if (response.code() == 404) {
            null
        } else {
            if (!response.isSuccessful) {
                throw AppError.ApiError(
                    status = response.code(),
                    uiCode = "error_api_${response.code()}",
                    message = response.message()
                )
            }

            response.body() ?: throw AppError.UnknownError
        }
    } catch (e: IOException) {
        println(e)
        throw AppError.NetworkError
    } catch (e: AppError) {
        // свои доменные ошибки не оборачиваем
        throw e
    } catch (e: Exception) {
        println(e)
        throw AppError.UnknownError
    }
}