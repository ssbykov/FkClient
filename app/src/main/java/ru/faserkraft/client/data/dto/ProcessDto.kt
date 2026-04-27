package ru.faserkraft.client.data.dto

import com.google.gson.annotations.SerializedName

data class ProcessDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("steps") val steps: List<StepDefinitionDto>
)
