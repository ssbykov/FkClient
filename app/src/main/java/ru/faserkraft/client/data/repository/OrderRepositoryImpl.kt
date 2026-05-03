package ru.faserkraft.client.data.repository

import ru.faserkraft.client.api.Api
import ru.faserkraft.client.data.callApi
import ru.faserkraft.client.data.callApiUnit
import ru.faserkraft.client.data.mapper.toCreateDto
import ru.faserkraft.client.data.mapper.toDomain
import ru.faserkraft.client.domain.model.Order
import ru.faserkraft.client.domain.model.OrderItem
import ru.faserkraft.client.domain.repository.OrderRepository
import ru.faserkraft.client.dto.OrderCreateDto
import ru.faserkraft.client.dto.OrderUpdateDto
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    private val api: Api,
) : OrderRepository {

    override suspend fun getAllOrders(): List<Order> =
        callApi { api.getAllOrders() }.orEmpty().map { it.toDomain() }

    override suspend fun getOrder(orderId: Int): Order =
        requireNotNull(callApi { api.getOrder(orderId) }).toDomain()

    override suspend fun createOrder(
        contractNumber: String,
        contractDate: String,
        plannedShipmentDate: String,
    ): Order = requireNotNull(
        callApi {
            api.createOrder(
                OrderCreateDto(
                    contractNumber = contractNumber,
                    contractDate = contractDate,
                    plannedShipmentDate = plannedShipmentDate,
                )
            )
        }
    ).toDomain()

    override suspend fun updateOrder(
        orderId: Int,
        contractNumber: String,
        contractDate: String,
        plannedShipmentDate: String,
    ): Order = requireNotNull(
        callApi {
            api.updateOrder(
                OrderUpdateDto(
                    id = orderId,
                    contractNumber = contractNumber,
                    contractDate = contractDate,
                    plannedShipmentDate = plannedShipmentDate,
                )
            )
        }
    ).toDomain()

    override suspend fun updateOrderItems(orderId: Int, items: List<OrderItem>): Order =
        requireNotNull(
            callApi { api.updateOrderItems(orderId, items.map { it.toCreateDto() }) }
        ).toDomain()

    override suspend fun closeOrder(orderId: Int): Order =
        requireNotNull(callApi { api.closeOrder(orderId) }).toDomain()

    override suspend fun deleteOrder(orderId: Int) =
        callApiUnit { api.deleteOrder(orderId) }

    override suspend fun addPackagingToOrder(orderId: Int, packagingIds: List<Int>) {
        callApi { api.addPackagingToOrder(orderId, packagingIds) }
    }

    override suspend fun detachPackagingFromOrder(packagingIds: List<Int>) {
        callApi { api.detachPackagingFromOrder(packagingIds) }
    }
}