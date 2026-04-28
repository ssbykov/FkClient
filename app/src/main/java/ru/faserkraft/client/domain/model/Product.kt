package ru.faserkraft.client.domain.model

data class Product(
    val id: Long,
    val serialNumber: String,
    val process: Process,
    val createdAt: String,
    val packagingId: Int?,
    val status: ProductStatus,
    val steps: List<Step>,
)

enum class ProductStatus { NORMAL, REWORK, SCRAP }

data class FinishedProduct(
    val id: Int,
    val serialNumber: String,
    val process: FinishedProcess,
)

data class ProductsInventory(
    val processId: Int,
    val processName: String,
    val stepDefinitionId: Int,
    val stepName: String,
    val stepNameGenitive: String,
    val count: Int,
)