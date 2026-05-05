package ru.faserkraft.client.data

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import ru.faserkraft.client.error.AppError
import java.io.IOException

private const val TAG = "BaseRequest"

private fun parseApiErrorBody(raw: String): Pair<String?, String?> {
    return try {
        if (raw.isBlank()) return null to null

        val json = JSONObject(raw)
        var detail: String? = null

        if (json.has("detail")) {
            val detailElement = json.get("detail")
            detail = if (detailElement is JSONArray) {
                if (detailElement.length() > 0) {
                    detailElement.getJSONObject(0).optString("msg")
                } else {
                    null
                }
            } else {
                detailElement.toString()
            }
        }

        val code = json.optString("code").takeIf { it.isNotBlank() }
        code to detail
    } catch (e: Exception) {
        Log.w(TAG, "Failed to parse api error body: $raw", e)
        null to null
    }
}

suspend fun <R> callApi(block: suspend () -> Response<R>): R? {
    return try {
        val response = block()

        if (!response.isSuccessful) {
            val raw = response.errorBody()?.string().orEmpty()
            val (serverCode, serverDetail) = parseApiErrorBody(raw)
            val errorMessage = serverDetail?.takeIf { it.isNotBlank() } ?: response.message()
            val uiCode = serverCode ?: "error_api_${response.code()}"

            Log.e(
                TAG,
                "HTTP ${response.code()} ${response.message()} body=$raw"
            )

            throw AppError.ApiError(
                status = response.code(),
                uiCode = uiCode,
                message = errorMessage,
            )
        }

        response.body()
    } catch (e: IOException) {
        Log.e(TAG, "Network IO error", e)
        throw AppError.NetworkError(e)
    } catch (e: AppError) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Unexpected error in callApi", e)
        throw AppError.UnknownError(e)
    }
}

suspend fun callApiUnit(block: suspend () -> Response<Unit>) {
    callApi<Unit>(block)
}