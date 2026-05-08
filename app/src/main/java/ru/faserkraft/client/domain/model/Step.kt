package ru.faserkraft.client.domain.model

data class Step(
    val id: Int,
    val productId: Int,
    val definition: StepDefinition,
    val status: StepStatus,
    val performedBy: Employee?,
    val performedAt: String?,
)

enum class StepStatus { DONE, PENDING }
