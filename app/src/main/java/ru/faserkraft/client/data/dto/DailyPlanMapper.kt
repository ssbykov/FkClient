package ru.faserkraft.client.data.dto

import ru.faserkraft.client.domain.model.DailyPlan
import ru.faserkraft.client.domain.model.DailyPlanStep

/**
 * Преобразование DTO в Domain модель для дневных планов
 */
fun DailyPlanDto.toDomain(): DailyPlan {
    return DailyPlan(
        id = id,
        date = date,
        employee = employee.toDomain(),
        steps = steps.map { it.toDomain() }
    )
}

fun DailyPlanStepDto.toDomain(): DailyPlanStep {
    return DailyPlanStep(
        id = id,
        stepDefinition = stepDefinition.toDomain(),
        workProcess = workProcess.toDomain(),
        plannedQuantity = plannedQuantity,
        actualQuantity = actualQuantity
    )
}

fun EmployeeDto.toDomain(): ru.faserkraft.client.domain.model.Employee {
    return ru.faserkraft.client.domain.model.Employee(
        id = id,
        name = name,
        role = "WORKER" // TODO: Get actual role from user
    )
}
