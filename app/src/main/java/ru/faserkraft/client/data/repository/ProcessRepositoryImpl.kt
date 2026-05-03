package ru.faserkraft.client.data.repository

import ru.faserkraft.client.api.Api
import ru.faserkraft.client.data.callApi
import ru.faserkraft.client.data.mapper.toDomain
import ru.faserkraft.client.domain.model.Process
import ru.faserkraft.client.domain.repository.ProcessRepository
import javax.inject.Inject

class ProcessRepositoryImpl @Inject constructor(
    private val api: Api,
) : ProcessRepository {

    override suspend fun getProcesses(): List<Process> =
        callApi { api.getProcesses() }.orEmpty().map { it.toDomain() }
}