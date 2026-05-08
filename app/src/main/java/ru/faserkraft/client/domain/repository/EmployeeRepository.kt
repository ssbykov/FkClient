package ru.faserkraft.client.domain.repository

import ru.faserkraft.client.domain.model.Employee

interface EmployeeRepository {
    suspend fun getEmployees(): List<Employee>
    suspend fun getEmployeeQrContent(employeeId: Int): String
}