package ru.faserkraft.client.error

import java.io.Serializable

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
}
