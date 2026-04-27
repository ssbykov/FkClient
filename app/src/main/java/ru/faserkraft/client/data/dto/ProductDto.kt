package ru.faserkraft.client.data.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO для товара от сервера
 */
data class ProductDto(
    @SerializedName("id") val id: Long,
    @SerializedName("serial_number") val serialNumber: String,
    @SerializedName("process_id") val processId: Int,
    @SerializedName("status") val status: String,
    @SerializedName("steps") val steps: List<StepDto>,
    @SerializedName("created_date") val createdDate: String?,
    @SerializedName("last_modified_date") val lastModifiedDate: String?
)

data class StepDto(
    @SerializedName("id") val id: Int,
    @SerializedName("status") val status: String,
    @SerializedName("step_definition") val stepDefinition: StepDefinitionDto
)

data class StepDefinitionDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("order") val order: Int
)
