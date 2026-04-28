package ru.faserkraft.client.domain.model

data class DailyPlan(
    val id: Int,
    val employeeId: Int,
    val date: String,
    val employee: Employee,
    val steps: List<DailyPlanStep>,
)

data class DailyPlanStep(
    val id: Int,
    val dailyPlanId: Int,
    val stepDefinitionId: Int,
    val plannedQuantity: Int,
    val actualQuantity: Int,
    val workProcess: String,
    val stepDefinition: StepDefinition,
)

data class DayPlans(
    val date: String,
    val plans: List<DailyPlan>?,
)