package ru.faserkraft.client.data

import com.google.gson.JsonParser
import retrofit2.Response
import ru.faserkraft.client.error.AppError
import ru.faserkraft.client.utils.logger.Logger
import java.io.IOException

private const val TAG = "BaseRequest"

private fun parseApiErrorBody(raw: String): Pair<String?, String?> {
    return try {
        if (raw.isBlank()) return null to null
        val json = JsonParser.parseString(raw).asJsonObject
        val detail = json.get("detail")?.let { d ->
            if (d.isJsonArray) d.asJsonArray.firstOrNull()?.asJsonObject?.get("msg")?.asString
            else d.asString
        }
        val code = json.get("code")?.asString?.takeIf { it.isNotBlank() }
        code to detail
    } catch (e: Exception) {
        null to null
    }
}

suspend fun <R> callApi(logger: Logger, block: suspend () -> Response<R>): R? {
    return try {
        val response = block()
        if (!response.isSuccessful) {
            val raw = response.errorBody()?.string().orEmpty()
            val (serverCode, serverDetail) = parseApiErrorBody(raw)
            val errorMessage = serverDetail?.takeIf { it.isNotBlank() } ?: response.message()
            val uiCode = serverCode ?: "error_api_${response.code()}"
            logger.e(TAG, "HTTP ${response.code()} ${response.message()} body=$raw")
            throw AppError.ApiError(
                status = response.code(),
                uiCode = uiCode,
                message = errorMessage
            )
        }
        response.body()
    } catch (e: IOException) {
        logger.e(TAG, "Network IO error", e)
        throw AppError.NetworkError(e)
    } catch (e: AppError) {
        throw e
    } catch (e: Exception) {
        logger.e(TAG, "Unexpected error in callApi", e)
        throw AppError.UnknownError(e)
    }
}

suspend fun callApiUnit(logger: Logger, block: suspend () -> Response<Unit>) {
    callApi(logger, block)
}