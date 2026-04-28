package ru.faserkraft.client.domain.usecase.product

import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.repository.ProductRepository
import javax.inject.Inject

class GetProductsByStepEmployeeDayUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(
        stepDefinitionId: Int,
        day: String,
        employeeId: Int,
    ): List<Product> = repository.getProductsByStepEmployeeDay(stepDefinitionId, day, employeeId)
}