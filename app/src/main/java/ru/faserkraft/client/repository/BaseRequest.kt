package ru.faserkraft.client.repository

import retrofit2.Response
import ru.faserkraft.client.error.AppError
import java.io.IOException
import org.json.JSONObject

import org.json.JSONArray

private fun parseApiErrorBody(raw: String): Pair<String?, String?> {
    return try {
        if (raw.isBlank()) return null to null

        val json = JSONObject(raw)
        var detail: String? = null

        if (json.has("detail")) {
            val detailElement = json.get("detail")
            detail = if (detailElement is JSONArray) {
                // Ошибка валидации Pydantic — это массив объектов
                if (detailElement.length() > 0) {
                    detailElement.getJSONObject(0).optString("msg")
                } else null
            } else {
                // Обычная ошибка FastAPI — это просто строка
                detailElement.toString()
            }
        }

        val code = json.optString("code").takeIf { it.isNotBlank() }
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

        if (!response.isSuccessful) {
            val raw = response.errorBody()?.string().orEmpty()
            val (serverCode, serverDetail) = parseApiErrorBody(raw)

            val errorMessage = serverDetail?.takeIf { it.isNotBlank() } ?: response.message()
            val uiCode = serverCode ?: "error_api_${response.code()}"

            throw AppError.ApiError(
                status = response.code(),
                uiCode = uiCode,
                message = errorMessage
            )
        }

        response.body() ?: throw AppError.UnknownError

    } catch (e: IOException) {
        throw AppError.NetworkError
    } catch (e: AppError) {
        throw e
    } catch (e: Exception) {
        throw AppError.UnknownError
    }
}

suspend fun callApiNoBody(
    block: suspend () -> Response<Unit>
) {
    try {
        val response = block()

        if (!response.isSuccessful) {
            val raw = response.errorBody()?.string().orEmpty()
            val (serverCode, serverDetail) = parseApiErrorBody(raw)

            val errorMessage = serverDetail?.takeIf { it.isNotBlank() }
                ?: if (response.code() == 404) "Не найдено" else response.message()

            val uiCode = serverCode ?: "error_api_${response.code()}"

            throw AppError.ApiError(
                status = response.code(),
                uiCode = uiCode,
                message = errorMessage
            )
        }

    } catch (e: IOException) {
        throw AppError.NetworkError
    } catch (e: AppError) {
        throw e
    } catch (e: Exception) {
        throw AppError.UnknownError
    }
}