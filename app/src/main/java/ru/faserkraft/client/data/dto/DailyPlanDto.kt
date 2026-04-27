package ru.faserkraft.client.data.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO для дневного плана от сервера
 */
data class DailyPlanDto(
    @SerializedName("id") val id: Int,
    @SerializedName("date") val date: String,
    @SerializedName("employee_id") val employeeId: Int,
    @SerializedName("employee") val employee: EmployeeDto,
    @SerializedName("steps") val steps: List<DailyPlanStepDto>
)

data class DailyPlanStepDto(
    @SerializedName("id") val id: Int,
    @SerializedName("step_definition") val stepDefinition: StepDefinitionDto,
    @SerializedName("work_process") val workProcess: ProcessDto,
    @SerializedName("planned_quantity") val plannedQuantity: Int,
    @SerializedName("actual_quantity") val actualQuantity: Int?
)
