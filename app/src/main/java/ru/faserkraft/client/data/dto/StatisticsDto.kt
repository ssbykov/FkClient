package ru.faserkraft.client.data.dto

import com.google.gson.annotations.SerializedName

data class StepCountStatDto(
    @SerializedName("process_id") val processId: Int,
    @SerializedName("process_name") val processName: String,
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

data class PeriodStatisticsDto(
    @SerializedName("finished_products") val finishedProducts: List<ProcessCountStatDto>,
    @SerializedName("total_steps") val totalSteps: List<StepCountStatDto>
)