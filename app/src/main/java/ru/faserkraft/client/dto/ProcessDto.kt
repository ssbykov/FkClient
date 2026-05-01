package ru.faserkraft.client.dto

import com.google.gson.annotations.SerializedName

data class ProcessDto(
    val id: Int,
    val name: String,
    val description: String? = null,
    val steps: List<StepDefinitionDto>? = null,
) : ItemDto()

data class FinishedProcessDto(
    val id: Int,
    val name: String,
    @SerializedName("size_type")
    val type: SizeType?,
) : ItemDto()
