package ru.faserkraft.client.domain.model

/**
 * Domain model for Process
 */
data class Process(
    val id: Int,
    val name: String,
    val description: String,
    val steps: List<StepDefinition>
)
