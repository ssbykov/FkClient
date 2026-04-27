package ru.faserkraft.client.domain.model

/**
 * Domain model for Order
 */
data class Order(
    val id: Int,
    val contractNumber: String,
    val contractDate: String,
    val plannedShipmentDate: String,
    val shipmentDate: String?,
    val status: String,
    val items: List<OrderItem>,
    val packaging: List<Packaging>,
    val createdDate: String?,
    val lastModifiedDate: String?
)

data class OrderItem(
    val id: Int,
    val productId: Long,
    val quantity: Int,
    val status: String,
    val workProcess: Process
)
