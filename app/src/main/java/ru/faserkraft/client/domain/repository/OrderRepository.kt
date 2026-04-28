package ru.faserkraft.client.domain.repository

import ru.faserkraft.client.domain.model.Order
import ru.faserkraft.client.domain.model.OrderItem

interface OrderRepository {
    suspend fun getAllOrders(): List<Order>
    suspend fun getOrder(orderId: Int): Order
    suspend fun createOrder(
        contractNumber: String,
        contractDate: String,
        plannedShipmentDate: String,
    ): Order
    suspend fun updateOrder(
        orderId: Int,
        contractNumber: String,
        contractDate: String,
        plannedShipmentDate: String,
    ): Order
    suspend fun updateOrderItems(orderId: Int, items: List<OrderItem>): Order
    suspend fun closeOrder(orderId: Int): Order
    suspend fun deleteOrder(orderId: Int)
    suspend fun addPackagingToOrder(orderId: Int, packagingIds: List<Int>)
    suspend fun detachPackagingFromOrder(packagingIds: List<Int>)
}