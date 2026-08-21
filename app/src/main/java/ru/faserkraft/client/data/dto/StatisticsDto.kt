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
    @SerializedName("count") val count: Int
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

data class PeriodStatisticsDto(
    @SerializedName("finished_products") val finishedProducts: List<ProcessCountStatDto>,
    @SerializedName("total_steps") val totalSteps: List<StepCountStatDto>,
    @SerializedName("employee_plans") val employeePlans: List<EmployeePlanStatDto>
)