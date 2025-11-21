package ru.faserkraft.client.error

import java.io.Serializable

sealed class AppError(open var code: String) : RuntimeException(), Serializable

class ApiError(val status: Int, override var code: String) : AppError(code), Serializable

object NetworkError : AppError("error_network")
object UnknownError : AppError("error_unknown")
object DaoError : AppError("dao_error")
