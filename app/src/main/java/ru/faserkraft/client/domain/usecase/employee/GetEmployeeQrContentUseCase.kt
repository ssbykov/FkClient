package ru.faserkraft.client.domain.usecase.employee

import ru.faserkraft.client.domain.repository.EmployeeRepository
import javax.inject.Inject

class GetEmployeeQrContentUseCase @Inject constructor(
    private val employeeRepository: EmployeeRepository
) {
    suspend operator fun invoke(employeeId: Int): String {
        return employeeRepository.getEmployeeQrContent(employeeId)
    }
}