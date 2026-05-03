package ru.faserkraft.client.domain.model

data class Packaging(
    val id: Int,
    val serialNumber: String,
    val performedBy: Employee?,
    val performedAt: String?,
    val orderId: Int?,
    val products: List<FinishedProduct> = emptyList(),
)