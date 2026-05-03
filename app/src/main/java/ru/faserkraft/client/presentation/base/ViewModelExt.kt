package ru.faserkraft.client.presentation.base

import ru.faserkraft.client.error.AppError

fun Throwable.toErrorMessage(): String = when (this) {
    is AppError.ApiError -> message ?: UNKNOWN_ERROR
    is AppError.NetworkError -> "Нет подключения к сети"
    is AppError -> message ?: UNKNOWN_ERROR
    else -> UNKNOWN_ERROR
}

private const val UNKNOWN_ERROR = "Неизвестная ошибка"