package ru.faserkraft.client.data.repository

import ru.faserkraft.client.data.mapper.toCreateDto
import ru.faserkraft.client.data.mapper.toDomain
import ru.faserkraft.client.domain.model.Order
import ru.faserkraft.client.domain.model.OrderItem
import ru.faserkraft.client.domain.repository.OrderRepository
import ru.faserkraft.client.dto.OrderCreateDto
import ru.faserkraft.client.dto.OrderUpdateDto
import ru.faserkraft.client.repository.ApiRepository
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    private val apiRepository: ApiRepository,
) : OrderRepository {

    override suspend fun getAllOrders(): List<Order> =
        apiRepository.getAllOrders().orEmpty().map { it.toDomain() }

    override suspend fun getOrder(orderId: Int): Order =
        requireNotNull(apiRepository.getOrder(orderId)).toDomain()

    override suspend fun createOrder(
        contractNumber: String,
        contractDate: String,
        plannedShipmentDate: String,
    ): Order = requireNotNull(
        apiRepository.createOrder(
            OrderCreateDto(
                contractNumber = contractNumber,
                contractDate = contractDate,
                plannedShipmentDate = plannedShipmentDate,
            )
        )
    ).toDomain()

    override suspend fun updateOrder(
        orderId: Int,
        contractNumber: String,
        contractDate: String,
        plannedShipmentDate: String,
    ): Order = requireNotNull(
        apiRepository.updateOrder(
            OrderUpdateDto(
                id = orderId,
                contractNumber = contractNumber,
                contractDate = contractDate,
                plannedShipmentDate = plannedShipmentDate,
            )
        )
    ).toDomain()

    override suspend fun updateOrderItems(orderId: Int, items: List<OrderItem>): Order =
        requireNotNull(
            apiRepository.updateOrderItems(orderId, items.map { it.toCreateDto() })
        ).toDomain()

    override suspend fun closeOrder(orderId: Int): Order =
        requireNotNull(apiRepository.closeOrder(orderId)).toDomain()

    override suspend fun deleteOrder(orderId: Int) {
        apiRepository.deleteOrder(orderId)
    }

    override suspend fun addPackagingToOrder(orderId: Int, packagingIds: List<Int>) {
        apiRepository.addPackagingToOrder(orderId, packagingIds)
    }

    override suspend fun detachPackagingFromOrder(packagingIds: List<Int>) {
        apiRepository.detachPackagingFromOrder(packagingIds)
    }
}