package ru.faserkraft.client.domain.model

data class Process(
    val id: Int,
    val name: String,
    val description: String,
    val steps: List<StepDefinition>,
)

// Облегчённая версия для FinishedProduct
data class FinishedProcess(
    val id: Int,
    val name: String,
    val sizeTypeName: String?,
    val packagingCount: Int?,
)