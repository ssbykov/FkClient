package ru.faserkraft.client.domain.model

import java.math.BigDecimal


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
    val count: Int,
    val totalAmount: BigDecimal
)

data class ProcessCountStat(
    val processId: Int,
    val processName: String,
    val count: Int
)

data class EmployeeEarningsStat(
    val employeeId: Int,
    val employeeName: String,
    val totalEarned: BigDecimal,
    val steps: List<StepCountStat>
)

data class PeriodStatistics(
    val totalWorkingDays: Int,
    val finishedProducts: List<ProcessCountStat>,
    val totalSteps: List<StepCountStat>,
    val employeePlans: List<EmployeePlanStat>,
    val employeeEarnings: List<EmployeeEarningsStat>,
    val totalEarnedAll: BigDecimal
)

data class EmployeePlanStat(
    val employeeId: Int,
    val employeeName: String,
    val workingDays: Int,
    val steps: List<DailyPlanStep>
)