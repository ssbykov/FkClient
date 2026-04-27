package ru.faserkraft.client.domain.model

/**
 * Domain model for Daily Plan
 */
data class DailyPlan(
    val id: Int,
    val date: String,
    val employee: Employee,
    val steps: List<DailyPlanStep>
)

data class DailyPlanStep(
    val id: Int,
    val stepDefinition: StepDefinition,
    val workProcess: Process,
    val plannedQuantity: Int,
    val actualQuantity: Int?
)
