package ru.faserkraft.client.dto

data class ProcessDto(
    val id: Int,
    val name: String,
    val description: String,
    val steps: List<StepDefinitionDto>,
) : ItemDto()
