package ru.faserkraft.client.dto

import com.google.gson.annotations.SerializedName

data class DayPlanStepDto(
    val id: Int,
    @SerializedName("daily_plan_id")
    val dailyPlanId: Int,
    @SerializedName("step_definition_id")
    val stepDefinitionId: Int,
    @SerializedName("planned_quantity")
    val plannedQuantity: Int,
    @SerializedName("actual_quantity")
    val actualQuantity: Int,
    @SerializedName("work_process")
    val workProcess: String,
    @SerializedName("step_definition")
    val stepDefinition: StepDefinitionDto
)

