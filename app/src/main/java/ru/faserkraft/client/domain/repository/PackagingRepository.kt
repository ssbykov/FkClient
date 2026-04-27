package ru.faserkraft.client.domain.repository

import ru.faserkraft.client.domain.model.FinishedProduct
import ru.faserkraft.client.domain.model.Packaging

/**
 * Repository interface для работы с упаковкой
 */
interface PackagingRepository {
    /**
     * Получить упаковку по серийному номеру
     */
    suspend fun getPackagingBySerialNumber(serialNumber: String): Result<Packaging>

    /**
     * Создать новую упаковку
     */
    suspend fun createPackaging(
        serialNumber: String,
        productIds: List<Long>
    ): Result<Packaging>

    /**
     * Получить упаковку на хранении
     */
    suspend fun getPackagingInStorage(): Result<List<Packaging>>

    /**
     * Удалить упаковку
     */
    suspend fun deletePackaging(serialNumber: String): Result<Unit>

    /**
     * Получить готовые товары для упаковки
     */
    suspend fun getFinishedProducts(): Result<List<FinishedProduct>>
}

