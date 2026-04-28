package ru.faserkraft.client.domain.repository

import ru.faserkraft.client.domain.model.FinishedProduct
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.model.ProductsInventory

interface ProductRepository {
    suspend fun getProduct(serialNumber: String): Product?
    suspend fun createProduct(serialNumber: String, processId: Int): Product
    suspend fun changeStatus(productId: Long, status: ProductStatus): Product
    suspend fun changeProcess(productId: Long, newProcessId: Int): Product
    suspend fun getProductsInventory(): List<ProductsInventory>
    suspend fun getFinishedProducts(): List<FinishedProduct>
    suspend fun getProductsByLastCompletedStep(
        processId: Int,
        stepDefinitionId: Int,
    ): List<Product>

    suspend fun getProductsByStepEmployeeDay(
        stepDefinitionId: Int,
        day: String,
        employeeId: Int,
    ): List<Product>
}