package ru.faserkraft.client.domain.model

data class Process(
    val id: Int,
    val name: String,
    val description: String,
    val steps: List<StepDefinition>,
)

data class FinishedProcess(
    val id: Int,
    val name: String,
    val sizeTypeId: Int?,
    val sizeTypeName: String?,
    val packagingCount: Int?,
)

data class ProcessShort(
    val id: Int,
    val name: String,
)