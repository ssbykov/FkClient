package ru.faserkraft.client.domain.usecase.employee

import ru.faserkraft.client.domain.model.Employee
import ru.faserkraft.client.domain.repository.EmployeeRepository
import javax.inject.Inject

class GetEmployeesUseCase @Inject constructor(
    private val repository: EmployeeRepository
) {
    suspend operator fun invoke(): List<Employee> =
        repository.getEmployees()
}