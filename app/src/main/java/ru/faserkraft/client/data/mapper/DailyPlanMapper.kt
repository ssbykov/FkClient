package ru.faserkraft.client.data.mapper

import ru.faserkraft.client.domain.model.DailyPlan
import ru.faserkraft.client.domain.model.DailyPlanStep
import ru.faserkraft.client.dto.DayPlanDto
import ru.faserkraft.client.dto.DayPlanStepDto

fun DayPlanStepDto.toDomain(): DailyPlanStep = DailyPlanStep(
    id = id,
    dailyPlanId = dailyPlanId,
    stepDefinitionId = stepDefinitionId,
    plannedQuantity = plannedQuantity,
    actualQuantity = actualQuantity,
    workProcess = workProcess,
    stepDefinition = stepDefinition.toDomain(),
)

fun DayPlanDto.toDomain(): DailyPlan = DailyPlan(
    id = id,
    employeeId = employeeId,
    date = date,
    employee = employee.toDomain(),
    steps = steps.map { it.toDomain() },
)