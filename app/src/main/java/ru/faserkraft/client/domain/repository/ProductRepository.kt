package ru.faserkraft.client.domain.repository

import ru.faserkraft.client.domain.model.Product

/**
 * Repository interface для работы с товарами
 * Абстрагирует источник данных (API, DB, cache)
 */
interface ProductRepository {
    /**
     * Получить товар по серийному номеру
     */
    suspend fun getProductBySerialNumber(serialNumber: String): Result<Product>

    /**
     * Получить товар по ID
     */
    suspend fun getProductById(id: Long): Result<Product>

    /**
     * Создать новый товар
     */
    suspend fun createProduct(
        serialNumber: String,
        processId: Int
    ): Result<Product>

    /**
     * Обновить статус товара
     */
    suspend fun updateProductStatus(
        productId: Long,
        status: String
    ): Result<Product>

    /**
     * Изменить процесс товара
     */
    suspend fun changeProductProcess(
        productId: Long,
        newProcessId: Int
    ): Result<Product>

    /**
     * Завершить шаг товара
     */
    suspend fun completeStep(stepId: Int): Result<Product>

    /**
     * Получить товары по завершённому шагу
     */
    suspend fun getProductsByLastCompletedStep(
        processId: Int,
        stepDefinitionId: Int
    ): Result<List<Product>>

    /**
     * Получить готовые товары для упаковки
     */
    suspend fun getFinishedProducts(): Result<List<Product>>
}

