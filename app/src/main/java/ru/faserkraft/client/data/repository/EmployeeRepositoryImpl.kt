package ru.faserkraft.client.data.repository

import ru.faserkraft.client.api.Api
import ru.faserkraft.client.data.callApi
import ru.faserkraft.client.data.mapper.toDomain
import ru.faserkraft.client.domain.model.Employee
import ru.faserkraft.client.domain.repository.EmployeeRepository
import ru.faserkraft.client.dto.toQrContent
import javax.inject.Inject

class EmployeeRepositoryImpl @Inject constructor(
    private val api: Api,
) : EmployeeRepository {

    override suspend fun getEmployees(): List<Employee> =
        callApi { api.getEmployees() }.orEmpty().map { it.toDomain() }

    override suspend fun getEmployeeQrContent(employeeId: Int): String {
        val response = callApi { api.getQrCode(employeeId) }
            ?: error("Пустой ответ от сервера")

        return response.toQrContent()
    }
}