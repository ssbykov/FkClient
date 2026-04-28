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

// Sentinel — заменяет emptyStep из StepDto
val EmptyStep = Step(
    id = 0,
    productId = 0,
    definition = StepDefinition(id = 0, order = 0, name = "", nameGenitive = ""),
    status = StepStatus.PENDING,
    performedBy = null,
    performedAt = null,
)