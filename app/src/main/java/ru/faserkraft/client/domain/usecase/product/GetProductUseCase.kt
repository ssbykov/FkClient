package ru.faserkraft.client.domain.usecase.product

import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.repository.ProductRepository
import javax.inject.Inject

/**
 * Use Case для получения товара
 * Инкапсулирует бизнес-логику получения товара
 */
class GetProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    /**
     * Получить товар по серийному номеру
     * @param serialNumber Серийный номер товара
     * @return Result с товаром или ошибкой
     */
    suspend operator fun invoke(serialNumber: String): Result<Product> {
        return productRepository.getProductBySerialNumber(serialNumber)
    }

    /**
     * Получить товар по ID
     */
    suspend fun getById(id: Long): Result<Product> {
        return productRepository.getProductById(id)
    }
}

