package ru.faserkraft.client.data.repository

import ru.faserkraft.client.data.dto.toQrContent
import ru.faserkraft.client.data.mapper.toDomain
import ru.faserkraft.client.data.network.Api
import ru.faserkraft.client.domain.model.Employee
import ru.faserkraft.client.domain.repository.EmployeeRepository
import ru.faserkraft.client.utils.logger.Logger
import javax.inject.Inject

class EmployeeRepositoryImpl @Inject constructor(
    private val api: Api,
    logger: Logger,
) : BaseRepository(logger), EmployeeRepository {

    override suspend fun getEmployees(): List<Employee> =
        callApi { api.getEmployees() }.orEmpty().map { it.toDomain() }

    override suspend fun getEmployeeQrContent(employeeId: Int): String {
        val response = callApi { api.getQrCode(employeeId) }
            ?: error("Пустой ответ от сервера")

        return response.toQrContent()
    }
}