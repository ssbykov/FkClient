package ru.faserkraft.client.error

import java.io.Serializable

/**
 * Sealed class для всех типов ошибок приложения
 */
sealed class AppError(
    open val uiCode: String,
    override val message: String? = null,
) : RuntimeException(message), Serializable {

    data class ApiError(
        val status: Int,
        override val uiCode: String,
        override val message: String? = null,
    ) : AppError(uiCode, message)

    data object NetworkError : AppError("error_network")
    data object UnknownError : AppError("error_unknown")
    data object DaoError : AppError("dao_error")

    companion object {
        fun fromException(e: Exception): AppError {
            return when (e) {
                is java.net.UnknownHostException,
                is java.net.ConnectException,
                is java.io.IOException -> NetworkError
                is retrofit2.HttpException -> ApiError(e.code(), "api_error", e.message())
                else -> UnknownError
            }
        }
    }
}
