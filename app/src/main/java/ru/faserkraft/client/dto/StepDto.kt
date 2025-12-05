package ru.faserkraft.client.dto

import com.google.gson.annotations.SerializedName

data class StepDto(
    val id: Int,
    @SerializedName("product_id")
    val productId: Int,
    @SerializedName("step_definition")
    val stepDefinition: StepDefinitionDto,
    val status: String,
    @SerializedName("performed_by_id")
    val performedById: Int?,
    @SerializedName("performed_by")
    val performedBy: EmployeeDto?,
    @SerializedName("performed_at")
    val performedAt: String?
)
