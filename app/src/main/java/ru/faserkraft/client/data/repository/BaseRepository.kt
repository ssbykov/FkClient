package ru.faserkraft.client.data.repository

import retrofit2.Response
import ru.faserkraft.client.data.callApi
import ru.faserkraft.client.data.callApiUnit
import ru.faserkraft.client.utils.logger.Logger

abstract class BaseRepository(
    protected val logger: Logger
) {
    protected suspend fun <R> callApi(block: suspend () -> Response<R>): R? =
        callApi(logger, block)

    protected suspend fun callApiUnit(block: suspend () -> Response<Unit>) =
        callApiUnit(logger, block)

    protected suspend fun <R> callApiOrThrow(
        errorMessage: String = "Server returned empty response",
        block: suspend () -> Response<R>,
    ): R = callApi(block) ?: error(errorMessage)
}