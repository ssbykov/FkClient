package ru.faserkraft.client.data.repository

import ru.faserkraft.client.data.api.OrderApi
import ru.faserkraft.client.data.dto.OrderCreateDto
import ru.faserkraft.client.data.dto.OrderUpdateDto
import ru.faserkraft.client.data.dto.toDomain
import ru.faserkraft.client.domain.model.Order
import ru.faserkraft.client.domain.repository.OrderRepository
import ru.faserkraft.client.error.AppError
import ru.faserkraft.client.utils.callApi
import javax.inject.Inject

/**
 * Реализация OrderRepository
 */
class OrderRepositoryImpl @Inject constructor(
    private val orderApi: OrderApi
) : OrderRepository {

    override suspend fun getAllOrders(): Result<List<Order>> {
        return try {
            val response = orderApi.getAllOrders()
            callApi(response).map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }

    override suspend fun getOrderById(orderId: Int): Result<Order> {
        return try {
            val response = orderApi.getOrder(orderId)
            callApi(response).map { it.toDomain() }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }

    override suspend fun createOrder(number: String): Result<Order> {
        return try {
            val request = OrderCreateDto(number)
            val response = orderApi.createOrder(request)
            callApi(response).map { it.toDomain() }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }

    override suspend fun updateOrder(orderId: Int, number: String): Result<Order> {
        return try {
            val request = OrderUpdateDto(orderId, number)
            val response = orderApi.updateOrder(orderId, request)
            callApi(response).map { it.toDomain() }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }

    override suspend fun closeOrder(orderId: Int): Result<Order> {
        return try {
            val response = orderApi.closeOrder(orderId)
            callApi(response).map { it.toDomain() }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }

    override suspend fun deleteOrder(orderId: Int): Result<Unit> {
        return try {
            val response = orderApi.deleteOrder(orderId)
            callApi(response).map { Unit }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }

    override suspend fun addPackagingToOrder(
        orderId: Int,
        packagingIds: List<Int>
    ): Result<Unit> {
        return try {
            val request = mapOf("packaging_ids" to packagingIds)
            val response = orderApi.addPackagingToOrder(orderId, request)
            callApi(response).map { Unit }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }

    override suspend fun detachPackagingFromOrder(packagingIds: List<Int>): Result<Unit> {
        return try {
            val request = mapOf("packaging_ids" to packagingIds)
            val response = orderApi.detachPackagingFromOrder(request)
            callApi(response).map { Unit }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }
}