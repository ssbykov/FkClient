package ru.faserkraft.client.data.repository

import ru.faserkraft.client.data.mapper.toDomain
import ru.faserkraft.client.domain.model.Employee
import ru.faserkraft.client.domain.repository.EmployeeRepository
import ru.faserkraft.client.repository.ApiRepository
import javax.inject.Inject

class EmployeeRepositoryImpl @Inject constructor(
    private val apiRepository: ApiRepository,
) : EmployeeRepository {

    override suspend fun getEmployees(): List<Employee> =
        apiRepository.getEmployees().orEmpty().map { it.toDomain() }
}