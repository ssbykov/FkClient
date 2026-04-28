package ru.faserkraft.client.domain.model

data class Order(
    val id: Int,
    val contractNumber: String,
    val contractDate: String,
    val plannedShipmentDate: String,
    val shipmentDate: String?,
    val shipmentBy: Employee?,
    val items: List<OrderItem>,
    val packaging: List<Packaging>,
)

data class OrderItem(
    val id: Int,
    val quantity: Int,
    val workProcess: Process,
)