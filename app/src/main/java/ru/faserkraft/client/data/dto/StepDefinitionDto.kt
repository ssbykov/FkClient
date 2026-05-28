package ru.faserkraft.client.data.dto

import com.google.gson.annotations.SerializedName

data class StepDefinitionDto(
    val id: Int,
    val order: Int,
    val template: TemplateDto
)

data class StepDefinitionWithProcessDto(
    val id: Int,
    val order: Int,
    @SerializedName("template")
    val template: TemplateDto,
    @SerializedName("work_process")
    val process: ProcessShortDto,
)