package ru.faserkraft.client.repository

import retrofit2.Response
import ru.faserkraft.client.error.ApiError
import ru.faserkraft.client.error.NetworkError
import ru.faserkraft.client.error.UnknownError
import java.io.IOException


suspend fun <R> callApi(
    block: suspend () -> Response<R>
): R {
    return try {
        val response = block()

        if (!response.isSuccessful) {
            throw ApiError(
                status = response.code(),
                code = response.message() // или свой uiCode
            )
        }

        response.body() ?: throw UnknownError
    } catch (e: IOException) {
        println(e)
        throw NetworkError
    } catch (e: ApiError) {
        throw e          // не оборачиваем
    } catch (e: Exception) {
        println(e)
        throw UnknownError
    }
}



