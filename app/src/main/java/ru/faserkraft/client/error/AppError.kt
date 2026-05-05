package ru.faserkraft.client.error

import java.io.Serializable

sealed class AppError(
    open val uiCode: String,
    override val message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause), Serializable {

    data class ApiError(
        val status: Int,
        override val uiCode: String,
        override val message: String? = null,
    ) : AppError(uiCode, message)

    data class NetworkError(
        val error: Throwable? = null,
    ) : AppError(
        uiCode = "error_network",
        message = error?.message,
        cause = error,
    )

    data class UnknownError(
        val error: Throwable? = null,
    ) : AppError(
        uiCode = "error_unknown",
        message = error?.message,
        cause = error,
    )

    data object DaoError : AppError("dao_error")
}