package ru.faserkraft.client.domain.repository

import ru.faserkraft.client.domain.model.Order

/**
 * Repository interface для работы с заказами
 */
interface OrderRepository {
    /**
     * Получить все заказы
     */
    suspend fun getAllOrders(): Result<List<Order>>

    /**
     * Получить заказ по ID
     */
    suspend fun getOrderById(orderId: Int): Result<Order>

    /**
     * Создать новый заказ
     */
    suspend fun createOrder(
        number: String
    ): Result<Order>

    /**
     * Обновить заказ
     */
    suspend fun updateOrder(
        orderId: Int,
        number: String
    ): Result<Order>

    /**
     * Закрыть заказ
     */
    suspend fun closeOrder(orderId: Int): Result<Order>

    /**
     * Удалить заказ
     */
    suspend fun deleteOrder(orderId: Int): Result<Unit>

    /**
     * Добавить упаковку в заказ
     */
    suspend fun addPackagingToOrder(
        orderId: Int,
        packagingIds: List<Int>
    ): Result<Unit>

    /**
     * Отдельить упаковку от заказа
     */
    suspend fun detachPackagingFromOrder(
        packagingIds: List<Int>
    ): Result<Unit>
}

