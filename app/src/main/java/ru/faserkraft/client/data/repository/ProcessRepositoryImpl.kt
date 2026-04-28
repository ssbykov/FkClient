package ru.faserkraft.client.data.repository

import ru.faserkraft.client.data.mapper.toDomain
import ru.faserkraft.client.domain.model.Process
import ru.faserkraft.client.domain.repository.ProcessRepository
import ru.faserkraft.client.repository.ApiRepository
import javax.inject.Inject

class ProcessRepositoryImpl @Inject constructor(
    private val apiRepository: ApiRepository,
) : ProcessRepository {

    override suspend fun getProcesses(): List<Process> =
        apiRepository.getProcesses().orEmpty().map { it.toDomain() }
}