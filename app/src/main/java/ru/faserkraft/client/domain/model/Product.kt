package ru.faserkraft.client.domain.model

/**
 * Domain model for Product
 * Не зависит от сервера, это наша внутренняя модель
 */
data class Product(
    val id: Long,
    val serialNumber: String,
    val processId: Int,
    val status: String,
    val steps: List<Step>,
    val createdDate: String?,
    val lastModifiedDate: String?
)

data class Step(
    val id: Int,
    val status: String,
    val stepDefinition: StepDefinition,
    val startedAt: String? = null,
    val completedAt: String? = null
)

data class StepDefinition(
    val id: Int,
    val name: String,
    val order: Int
)

