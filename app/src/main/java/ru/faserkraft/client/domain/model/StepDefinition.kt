package ru.faserkraft.client.domain.model

data class StepDefinition(
    val id: Int,
    val order: Int,
    val name: String,
    val nameGenitive: String,
)

data class StepDefinitionWithProcess(
    val id: Int,
    val order: Int,
    val name: String,
    val nameGenitive: String,
    val process: ProcessShort,
)