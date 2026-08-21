package ru.faserkraft.client.domain.model


data class StepCountStat(
    val processId: Int,
    val processName: String,
    val sizeTypeId: Int,
    val sizeTypeName: String,
    val stepDefinitionId: Int,
    val order: Int,
    val stepName: String,
    val employeeId: Int,
    val employeeName: String,
    val count: Int
)

data class ProcessCountStat(
    val processId: Int,
    val processName: String,
    val count: Int
)

data class PeriodStatistics(
    val finishedProducts: List<ProcessCountStat>,
    val totalSteps: List<StepCountStat>,
    val employeePlans: List<EmployeePlanStat>
)

data class EmployeePlanStat(
    val employeeId: Int,
    val employeeName: String,
    val workingDays: Int,
    val steps: List<DailyPlanStep>
)