package ru.faserkraft.client.data.dto

import com.google.gson.annotations.SerializedName

data class StepCountStatDto(
    @SerializedName("process_id") val processId: Int,
    @SerializedName("process_name") val processName: String,
    @SerializedName("size_type_id") val sizeTypeId: Int,
    @SerializedName("size_type_name") val sizeTypeName: String,
    @SerializedName("step_definition_id") val stepDefinitionId: Int,
    @SerializedName("order") val order: Int,
    @SerializedName("step_name") val stepName: String,
    @SerializedName("employee_id") val employeeId: Int,
    @SerializedName("employee_name") val employeeName: String,
    @SerializedName("count") val count: Int,
    @SerializedName("total_amount") val totalAmount: String
)

data class ProcessCountStatDto(
    @SerializedName("process_id") val processId: Int,
    @SerializedName("process_name") val processName: String,
    @SerializedName("count") val count: Int
)

data class EmployeePlanStatDto(
    @SerializedName("employee_id") val employeeId: Int,
    @SerializedName("employee_name") val employeeName: String,
    @SerializedName("working_days") val workingDays: Int,
    @SerializedName("steps") val steps: List<DayPlanStepDto>
)

data class EmployeeEarningsDto(
    @SerializedName("employee_id") val employeeId: Int,
    @SerializedName("employee_name") val employeeName: String,
    @SerializedName("total_earned") val totalEarned: String,
    @SerializedName("steps") val steps: List<StepCountStatDto>
)

data class PeriodStatisticsDto(
    @SerializedName("total_working_days") val totalWorkingDays: Int = 0,
    @SerializedName("finished_products") val finishedProducts: List<ProcessCountStatDto>,
    @SerializedName("total_steps") val totalSteps: List<StepCountStatDto>,
    @SerializedName("employee_plans") val employeePlans: List<EmployeePlanStatDto>,
    @SerializedName("employee_earnings") val employeeEarnings: List<EmployeeEarningsDto>,
    @SerializedName("total_earned_all") val totalEarnedAll: String,
)