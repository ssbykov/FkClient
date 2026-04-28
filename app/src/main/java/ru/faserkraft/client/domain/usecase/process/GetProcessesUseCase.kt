package ru.faserkraft.client.domain.usecase.process

import ru.faserkraft.client.domain.model.Process
import ru.faserkraft.client.domain.repository.ProcessRepository
import javax.inject.Inject

class GetProcessesUseCase @Inject constructor(
    private val repository: ProcessRepository
) {
    suspend operator fun invoke(): List<Process> =
        repository.getProcesses()
}