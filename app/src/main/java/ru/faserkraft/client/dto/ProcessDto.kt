package ru.faserkraft.client.dto

data class ProcessDto(
    val id: Int,
    val name: String,
    val description: String,
    val steps: List<StepDefinitionDto>,
) : ItemDto()

data class FinishedProcessDto(
    val id: Int,
    val name: String,
) : ItemDto()
