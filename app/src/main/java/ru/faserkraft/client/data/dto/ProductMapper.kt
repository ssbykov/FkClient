package ru.faserkraft.client.data.dto

import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.Step
import ru.faserkraft.client.domain.model.StepDefinition

/**
 * Преобразование DTO в Domain модель
 */
fun ProductDto.toDomain(): Product {
    return Product(
        id = id,
        serialNumber = serialNumber,
        processId = processId,
        status = status,
        steps = steps.map { it.toDomain() },
        createdDate = createdDate,
        lastModifiedDate = lastModifiedDate
    )
}

fun StepDto.toDomain(): Step {
    return Step(
        id = id,
        status = status,
        stepDefinition = stepDefinition.toDomain()
    )
}

fun StepDefinitionDto.toDomain(): StepDefinition {
    return StepDefinition(
        id = id,
        name = name,
        order = order
    )
}
