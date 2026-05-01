package ru.faserkraft.client.domain.model

data class Order(
    val id: Int,
    val contractNumber: String,
    val contractDate: String,
    val plannedShipmentDate: String,
    val shipmentDate: String?,
    val shipmentBy: Employee?,
    val items: List<OrderItem> = emptyList(),
    val packaging: List<Packaging> = emptyList(),
)

data class OrderItem(
    val id: Int,
    val quantity: Int,
    val workProcess: Process,
)