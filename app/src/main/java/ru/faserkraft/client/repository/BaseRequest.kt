package ru.faserkraft.client.repository

import retrofit2.Response
import ru.faserkraft.client.error.AppError
import java.io.IOException
import org.json.JSONObject

private fun parseApiErrorBody(raw: String): Pair<String?, String?> {
    return try {
        val json = JSONObject(raw)
        val detail = json.optString("detail")
        val code = json.optString("code")
        code to detail
    } catch (e: Exception) {
        null to null
    }
}

suspend fun <R> callApi(
    block: suspend () -> Response<R>
): R? {
    return try {
        val response = block()

        if (response.code() == 404) {
            val raw = response.errorBody()?.string().orEmpty()
            val (serverCode, serverDetail) = parseApiErrorBody(raw)

            throw AppError.ApiError(
                status = 404,
                uiCode = serverCode ?: "error_api_404",
                message = serverDetail ?: "Не найдено"
            )
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
        throw AppError.NetworkError
    } catch (e: AppError) {
        // свои доменные ошибки не оборачиваем
        throw e
    } catch (e: Exception) {
        throw AppError.UnknownError
    }
}

