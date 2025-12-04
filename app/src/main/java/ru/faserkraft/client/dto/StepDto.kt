package ru.faserkraft.client.dto

import com.google.gson.annotations.SerializedName

data class StepDto(
    val id: Int,
    @SerializedName("product_id")
    val productId: Int,
    @SerializedName("step_definition")
    val stepDefinition: StepDefinitionDto,
    val status: String,
    @SerializedName("accepted_by_id")
    val acceptedById: Int?,
    @SerializedName("accepted_at")
    val acceptedAt: String?,
    @SerializedName("performed_by_id")
    val performedById: Int?,
    @SerializedName("performed_at")
    val performedAt: String?
)
