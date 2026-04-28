package ru.faserkraft.client.domain.repository

import ru.faserkraft.client.domain.model.Process

interface ProcessRepository {
    suspend fun getProcesses(): List<Process>
}