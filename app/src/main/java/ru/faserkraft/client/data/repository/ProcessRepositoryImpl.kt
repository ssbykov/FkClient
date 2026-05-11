package ru.faserkraft.client.data.repository


import ru.faserkraft.client.data.mapper.toDomain
import ru.faserkraft.client.data.network.Api
import ru.faserkraft.client.domain.model.Process
import ru.faserkraft.client.domain.repository.ProcessRepository
import ru.faserkraft.client.utils.Logger
import javax.inject.Inject

class ProcessRepositoryImpl @Inject constructor(
    private val api: Api,
    logger: Logger,
) : BaseRepository(logger), ProcessRepository {

    override suspend fun getProcesses(): List<Process> =
        callApi { api.getProcesses() }.orEmpty().map { it.toDomain() }
}